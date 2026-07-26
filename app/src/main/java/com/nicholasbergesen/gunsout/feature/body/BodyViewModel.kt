package com.nicholasbergesen.gunsout.feature.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicholasbergesen.gunsout.auth.CurrentUserIdProvider
import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.data.prefs.UserPreferences
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import com.nicholasbergesen.gunsout.data.repo.BodyRepository
import com.nicholasbergesen.gunsout.domain.kcal.KcalTrendAnalyzer
import com.nicholasbergesen.gunsout.domain.nutrition.CalorieTargetCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BodyUiState(
    val profile: UserProfile = UserProfile(),
    val logs: List<BodyMetricsLog> = emptyList()
)

sealed interface BodyUiEvent {
    data class InBodyImported(
        val message: String,
        val undo: InBodyQrImportUndo
    ) : BodyUiEvent

    data class InBodyCsvImported(
        val message: String,
        val undo: InBodyCsvImportUndo
    ) : BodyUiEvent

    data class Message(val message: String) : BodyUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BodyViewModel @Inject constructor(
    private val body: BodyRepository,
    private val userPrefs: UserPreferences,
    private val currentUserIdProvider: CurrentUserIdProvider,
    private val inBodyQrImportUseCase: InBodyQrImportUseCase,
    private val inBodyCsvImportUseCase: InBodyCsvImportUseCase
) : ViewModel() {

    private val _events = MutableSharedFlow<BodyUiEvent>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<BodyUiEvent> = _events.asSharedFlow()
    private var inBodyImportInFlight = false

    val state: StateFlow<BodyUiState> = currentUserIdProvider.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            combine(
                userPrefs.profile(userId),
                body.observeSince(userId, LocalDate.now().minusYears(2))
            ) { profile, logs ->
                val sortedLogs = logs.sortedBy { it.date }
                // Keep the displayed "current weight" anchored to the latest logged row, falling
                // back to the persisted profile only when no logs exist yet. This stops the body
                // screen showing a 100 kg "current" alongside an 80 kg "latest" because they came
                // from different sources.
                val effectiveProfile = sortedLogs.lastOrNull()?.let {
                    profile.copy(currentBodyWeightKg = it.weightKg)
                } ?: profile
                BodyUiState(effectiveProfile, sortedLogs)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyUiState())

    fun logToday(
        weightKg: Double,
        bodyFatPct: Double?,
        muscleMassKg: Double?,
        waterLiters: Double?,
        visceralFatRating: Int?
    ) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        body.log(
            userId = userId,
            date = LocalDate.now(),
            weightKg = weightKg,
            bodyFatPct = bodyFatPct,
            muscleMassKg = muscleMassKg,
            waterLiters = waterLiters,
            visceralFatRating = visceralFatRating
        )
        userPrefs.update(userId) { it.copy(currentBodyWeightKg = weightKg) }
    }

    fun importInBodyQr(rawQrValue: String) {
        if (inBodyImportInFlight) return
        inBodyImportInFlight = true
        viewModelScope.launch {
            try {
                val userId = currentUserIdProvider.requireUserId()
                when (val result = inBodyQrImportUseCase.import(userId, rawQrValue)) {
                    is InBodyQrImportResult.Failed -> _events.tryEmit(BodyUiEvent.Message(result.message))
                    is InBodyQrImportResult.Imported -> _events.tryEmit(
                        BodyUiEvent.InBodyImported(
                            message = result.message,
                            undo = result.undo
                        )
                    )
                }
            } finally {
                inBodyImportInFlight = false
            }
        }
    }

    fun importInBodyCsv(rawCsv: String) {
        if (inBodyImportInFlight) return
        inBodyImportInFlight = true
        viewModelScope.launch {
            try {
                val userId = currentUserIdProvider.requireUserId()
                when (val result = inBodyCsvImportUseCase.import(userId, rawCsv)) {
                    is InBodyCsvImportResult.Failed -> _events.tryEmit(BodyUiEvent.Message(result.message))
                    is InBodyCsvImportResult.Imported -> _events.tryEmit(
                        BodyUiEvent.InBodyCsvImported(
                            message = result.message,
                            undo = result.undo
                        )
                    )
                }
            } finally {
                inBodyImportInFlight = false
            }
        }
    }

    fun undoInBodyImport(undo: InBodyQrImportUndo) = viewModelScope.launch {
        inBodyQrImportUseCase.undo(undo)
        _events.tryEmit(BodyUiEvent.Message("InBody import undone"))
    }

    fun undoInBodyCsvImport(undo: InBodyCsvImportUndo) = viewModelScope.launch {
        inBodyCsvImportUseCase.undo(undo)
        _events.tryEmit(BodyUiEvent.Message("InBody CSV import undone"))
    }

    fun showMessage(message: String) {
        _events.tryEmit(BodyUiEvent.Message(message))
    }

    fun suggestKcalAdjustment() = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        val current = state.value
        val overrides = userPrefs.targetOverrides(userId).first()
        val effective = CalorieTargetCalculator.effective(current.profile, overrides.kcal)
        if (effective == null) {
            _kcalSuggestion.value = KcalTrendAnalyzer.Suggestion(
                text = "Add your age, sex, height, current weight, and goal weight in Settings, or set a manual kcal override there, before asking for a suggestion.",
                newKcalTarget = null,
                ratePerWeekKg = null
            )
            return@launch
        }
        _kcalSuggestion.value = KcalTrendAnalyzer.analyze(
            logs = current.logs,
            currentTargetKcal = effective.kcal,
            currentWeightKg = current.profile.currentBodyWeightKg,
            goalWeightKg = current.profile.goalBodyWeightKg
        )
    }

    fun applyKcalSuggestion() = viewModelScope.launch {
        val suggestion = _kcalSuggestion.value ?: return@launch
        val newTarget = suggestion.newKcalTarget ?: return@launch
        val userId = currentUserIdProvider.requireUserId()
        userPrefs.updateTargetOverrides(userId) { it.copy(kcal = newTarget) }
        _kcalSuggestion.value = null
    }

    fun dismissKcalSuggestion() {
        _kcalSuggestion.value = null
    }

    private val _kcalSuggestion = kotlinx.coroutines.flow.MutableStateFlow<KcalTrendAnalyzer.Suggestion?>(null)
    val kcalSuggestion: StateFlow<KcalTrendAnalyzer.Suggestion?> = _kcalSuggestion
}

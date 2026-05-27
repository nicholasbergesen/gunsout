package com.gunsout.feature.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.auth.CurrentUserIdProvider
import com.gunsout.data.entity.BodyMetricsLog
import com.gunsout.data.prefs.UserPreferences
import com.gunsout.data.prefs.UserProfile
import com.gunsout.data.repo.BodyRepository
import com.gunsout.domain.kcal.KcalTrendAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BodyUiState(
    val profile: UserProfile = UserProfile(),
    val logs: List<BodyMetricsLog> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BodyViewModel @Inject constructor(
    private val body: BodyRepository,
    private val userPrefs: UserPreferences,
    private val currentUserIdProvider: CurrentUserIdProvider
) : ViewModel() {

    // userPrefs.profile stays single-user in Phase 2b-2; it becomes per-user in Phase 3 when the
    // DataStore router lands. The Room-backed body logs are already scoped by userId here.
    val state: StateFlow<BodyUiState> = currentUserIdProvider.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            combine(
                userPrefs.profile,
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
        waterPct: Double?,
        boneMassKg: Double?,
        visceralFatRating: Int?
    ) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        body.log(
            userId = userId,
            date = LocalDate.now(),
            weightKg = weightKg,
            bodyFatPct = bodyFatPct,
            muscleMassKg = muscleMassKg,
            waterPct = waterPct,
            boneMassKg = boneMassKg,
            visceralFatRating = visceralFatRating
        )
        userPrefs.update { it.copy(currentBodyWeightKg = weightKg) }
    }

    // TODO Phase 3: rebuild kcal suggestion against UserPreferences.overrideKcal once macro
    // targets are driven by MacroTargetCalculator instead of MealPlan.
    fun suggestKcalAdjustment() = viewModelScope.launch {
        _kcalSuggestion.value = null
    }

    // TODO Phase 3: write the suggested kcal to UserPreferences.overrideKcal.
    fun applyKcalSuggestion() = viewModelScope.launch {
        _kcalSuggestion.value = null
    }

    fun dismissKcalSuggestion() {
        _kcalSuggestion.value = null
    }

    private val _kcalSuggestion = kotlinx.coroutines.flow.MutableStateFlow<KcalTrendAnalyzer.Suggestion?>(null)
    val kcalSuggestion: StateFlow<KcalTrendAnalyzer.Suggestion?> = _kcalSuggestion
}

package com.gunsout.feature.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.data.entity.BodyMetricsLog
import com.gunsout.data.entity.MealPlan
import com.gunsout.data.prefs.UserPreferences
import com.gunsout.data.prefs.UserProfile
import com.gunsout.data.repo.BodyRepository
import com.gunsout.data.repo.DietRepository
import com.gunsout.data.repo.MealPlanRepository
import com.gunsout.domain.kcal.KcalTrendAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BodyUiState(
    val profile: UserProfile = UserProfile(),
    val logs: List<BodyMetricsLog> = emptyList(),
    val activeMealPlan: MealPlan? = null,
    val kcalSuggestion: KcalTrendAnalyzer.Suggestion? = null
)

@HiltViewModel
class BodyViewModel @Inject constructor(
    private val body: BodyRepository,
    private val userPrefs: UserPreferences,
    private val diet: DietRepository,
    private val mealPlanRepo: MealPlanRepository
) : ViewModel() {

    val state: StateFlow<BodyUiState> = combine(
        userPrefs.profile,
        body.observeSince(LocalDate.now().minusYears(2)),
        diet.observeActivePlan()
    ) { profile, logs, plan ->
        val sortedLogs = logs.sortedBy { it.date }
        BodyUiState(profile, sortedLogs, plan)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyUiState())

    fun logToday(
        weightKg: Double,
        bodyFatPct: Double?,
        muscleMassKg: Double?,
        waterPct: Double?,
        boneMassKg: Double?,
        visceralFatRating: Int?
    ) = viewModelScope.launch {
        body.log(
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

    /** Compute and surface a kcal target adjustment based on recent weight trend. */
    fun suggestKcalAdjustment() = viewModelScope.launch {
        val s = state.value
        val plan = s.activeMealPlan ?: return@launch
        val suggestion = KcalTrendAnalyzer.analyze(
            logs = s.logs,
            currentTargetKcal = plan.kcalTarget,
            currentWeightKg = s.profile.currentBodyWeightKg,
            goalWeightKg = s.profile.goalBodyWeightKg
        )
        // Re-emit by setting the suggestion through the state; we use a small side-channel here
        // since BodyUiState comes from combine().
        _kcalSuggestion.value = suggestion
    }

    /** Apply the latest suggested kcal target to the active meal plan. */
    fun applyKcalSuggestion() = viewModelScope.launch {
        val sug = _kcalSuggestion.value ?: return@launch
        val newTarget = sug.newKcalTarget ?: return@launch
        val plan = state.value.activeMealPlan ?: return@launch
        mealPlanRepo.updatePlan(plan.copy(kcalTarget = newTarget))
        _kcalSuggestion.value = null
    }

    fun dismissKcalSuggestion() {
        _kcalSuggestion.value = null
    }

    private val _kcalSuggestion = kotlinx.coroutines.flow.MutableStateFlow<KcalTrendAnalyzer.Suggestion?>(null)
    val kcalSuggestion: StateFlow<KcalTrendAnalyzer.Suggestion?> = _kcalSuggestion
}

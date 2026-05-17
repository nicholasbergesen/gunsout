package com.gunsout.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.data.entity.ProgramDay
import com.gunsout.data.entity.WorkoutSession
import com.gunsout.data.repo.WorkoutRepository
import com.gunsout.domain.schedule.ScheduleResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TodayUiState(
    val loading: Boolean = true,
    val programName: String? = null,
    val nextDay: ProgramDay? = null,
    val alternativeForToday: ProgramDay? = null,
    val allNonRestDays: List<ProgramDay> = emptyList(),
    val onSchedule: Boolean = false,
    val daysSinceLastSession: Int? = null,
    val lastSessionLabel: String? = null,
    val completedThisWeek: Int = 0,
    val sessionsTargetThisWeek: Int = 4,
    val baselineWeekActive: Boolean = true,
    val toast: String? = null
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val workouts: WorkoutRepository,
    private val userPrefs: com.gunsout.data.prefs.UserPreferences
) : ViewModel() {
    private val resolver = ScheduleResolver()

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val profile = userPrefs.profile.first()
        val days = workouts.getActiveProgramDays()
        val recent = workouts.getRecentSessions().sortedByDescending { it.date }
        val suggestion = resolver.resolveNext(days, recent, LocalDate.now())
        val weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY)
        val completedThisWeek = recent.count {
            it.status == com.gunsout.data.entity.SessionStatus.COMPLETED &&
                it.date >= weekStart &&
                days.firstOrNull { d -> d.id == it.programDayId }?.isRest != true
        }
        val sessionsTarget = days.count { !it.isRest }
        val lastSession = recent.firstOrNull { it.status == com.gunsout.data.entity.SessionStatus.COMPLETED }
        _state.update {
            it.copy(
                loading = false,
                nextDay = suggestion.nextDay,
                alternativeForToday = suggestion.alternativeForToday,
                allNonRestDays = days.filter { d -> !d.isRest },
                onSchedule = suggestion.onSchedule,
                daysSinceLastSession = suggestion.daysSinceLastSession,
                lastSessionLabel = lastSession?.programDayLabelSnapshot,
                completedThisWeek = completedThisWeek,
                sessionsTargetThisWeek = sessionsTarget,
                baselineWeekActive = profile.baselineWeekActive
            )
        }
    }

    fun startSession(day: ProgramDay, onCreated: (Long) -> Unit) = viewModelScope.launch {
        val id = workouts.startSession(day)
        onCreated(id)
    }

    fun markRestDay() = viewModelScope.launch {
        workouts.markRestDay()
        _state.update { it.copy(toast = "Today marked as rest day.") }
        refresh()
    }

    fun skipNextDay() = viewModelScope.launch {
        val day = _state.value.nextDay ?: return@launch
        workouts.skipNextDay(day)
        _state.update { it.copy(toast = "${day.label} skipped.") }
        refresh()
    }

    fun consumeToast() { _state.update { it.copy(toast = null) } }
}

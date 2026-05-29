package com.gunsout.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.auth.CurrentUserIdProvider
import com.gunsout.data.entity.ProgramDay
import com.gunsout.data.entity.WorkoutSession
import com.gunsout.data.repo.WorkoutRepository
import com.gunsout.domain.schedule.ScheduleResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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
    val baselineWeekActive: Boolean = true
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val workouts: WorkoutRepository,
    private val userPrefs: com.gunsout.data.prefs.UserPreferences,
    private val currentUserIdProvider: CurrentUserIdProvider
) : ViewModel() {
    private val resolver = ScheduleResolver()

    private val refreshTicker = MutableStateFlow(0)

    val state: StateFlow<TodayUiState> = currentUserIdProvider.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            val activeProgramFlow = workouts.observeActiveProgram(userId).distinctUntilChanged()
            val daysFlow = activeProgramFlow.flatMapLatest { program ->
                if (program == null) flowOf(emptyList()) else workouts.observeDaysFor(program.id)
            }
            val recentSessionsFlow = workouts.observeRecentCompletedAndSkipped(userId, limit = 50)
            combine(
                daysFlow,
                recentSessionsFlow,
                userPrefs.profile(userId),
                activeProgramFlow,
                refreshTicker
            ) { days, recent, profile, program, _ ->
                compute(days, recent, profile.baselineWeekActive, program?.createdAt)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    private fun compute(
        days: List<ProgramDay>,
        recent: List<WorkoutSession>,
        baselineFlag: Boolean,
        programCreatedAt: Long?
    ): TodayUiState {
        val sorted = recent.sortedByDescending { it.date }
        val suggestion = resolver.resolveNext(days, sorted, LocalDate.now())
        val weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY)
        val nonRestIds = days.filter { !it.isRest }.map { it.id }.toSet()
        val completedThisWeek = sorted.count {
            it.status == com.gunsout.data.entity.SessionStatus.COMPLETED &&
                it.date >= weekStart &&
                it.programDayId != null &&
                it.programDayId in nonRestIds
        }
        val sessionsTarget = days.count { !it.isRest }
        val lastSession = sorted.firstOrNull { it.status == com.gunsout.data.entity.SessionStatus.COMPLETED }
        val baselineActive = com.gunsout.domain.baseline.BaselineWeekResolver.isActive(
            programCreatedAt = programCreatedAt,
            forcedFlag = baselineFlag
        )
        return TodayUiState(
            loading = false,
            nextDay = suggestion.nextDay,
            alternativeForToday = suggestion.alternativeForToday,
            allNonRestDays = days.filter { !it.isRest },
            onSchedule = suggestion.onSchedule,
            daysSinceLastSession = suggestion.daysSinceLastSession,
            lastSessionLabel = lastSession?.programDayLabelSnapshot,
            completedThisWeek = completedThisWeek,
            sessionsTargetThisWeek = sessionsTarget,
            baselineWeekActive = baselineActive
        )
    }

    /** Trigger a recomposition with today's date; useful after resume or midnight rollover. */
    fun refresh() {
        refreshTicker.update { it + 1 }
    }

    fun startSession(day: ProgramDay, onCreated: (Long) -> Unit) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        val id = workouts.startSession(userId, day)
        onCreated(id)
    }

    fun markRestDay() = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        workouts.markRestDay(userId)
        refresh()
    }

    fun skipNextDay() = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        val day = state.value.nextDay ?: return@launch
        workouts.skipNextDay(userId, day)
        refresh()
    }
}

package com.gunsout.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.auth.CurrentUserIdProvider
import com.gunsout.data.entity.Exercise
import com.gunsout.data.entity.ProgramExercise
import com.gunsout.data.entity.SetEntry
import com.gunsout.data.entity.WorkoutSession
import com.gunsout.data.prefs.UserPreferences
import com.gunsout.data.repo.WorkoutRepository
import com.gunsout.domain.progression.ProgressionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class PlannedExerciseUi(
    val programExercise: ProgramExercise,
    val exercise: Exercise,
    val sets: List<SetEntry>,
    val previousBest: SetEntry?,
    val suggestion: ProgressionEngine.Suggestion?,
    val alternates: List<Exercise> = emptyList()
)

data class SessionUiState(
    val loading: Boolean = true,
    val session: WorkoutSession? = null,
    val dayLabel: String = "",
    val items: List<PlannedExerciseUi> = emptyList(),
    val baselineWeekActive: Boolean = true,
    val kneeFeel: Int? = null,
    val notes: String = "",
    val finished: Boolean = false
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workouts: WorkoutRepository,
    private val userPrefs: UserPreferences,
    private val currentUserIdProvider: CurrentUserIdProvider,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    private val engine = ProgressionEngine()
    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    /**
     * In-session swap overrides: maps programExerciseId -> exerciseId to use just for this
     * session. Persisted swaps (save to program) are written directly to the ProgramExercise row,
     * so they do not appear here.
     */
    private val sessionOverrides: MutableMap<Long, Long> = mutableMapOf()

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        val profile = userPrefs.profile(userId).first()
        val session = workouts.getSessionById(sessionId) ?: return@launch
        val pdId = session.programDayId ?: return@launch
        val activeProgram = workouts.getActiveProgram(userId)
        val baseline = com.gunsout.domain.baseline.BaselineWeekResolver.isActive(
            programCreatedAt = activeProgram?.createdAt,
            forcedFlag = profile.baselineWeekActive
        )
        val pds = workouts.getProgramExercises(pdId)
        val items = pds.map { pe ->
            val effectiveId = sessionOverrides[pe.id] ?: pe.exerciseId
            val ex = workouts.getExercise(effectiveId)
                ?: Exercise(
                    id = effectiveId,
                    userId = userId,
                    name = "(unknown)",
                    primaryMuscleGroup = com.gunsout.data.entity.MuscleGroup.OTHER,
                    equipment = com.gunsout.data.entity.Equipment.OTHER
                )
            val recent = workouts.getPreviousSetsForExercise(userId, ex.id)
            // Group recent sets into prior-session batches and order them by the session date so
            // backup-restored IDs (which may be out of chronological order) still surface the
            // genuinely most recent session.
            val priorSessions = recent
                .filter { it.sessionId != sessionId }
                .groupBy { it.sessionId }
                .mapNotNull { (sid, sets) ->
                    val date = workouts.getSessionById(sid)?.date ?: return@mapNotNull null
                    Triple(date, sid, sets)
                }
                .sortedByDescending { it.first }
            val priorBest = priorSessions.firstOrNull()?.third
                ?.maxByOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
            val priorSets = priorSessions.firstOrNull()?.third.orEmpty()
            val suggestion = engine.suggest(pe, ex, priorSets, baseline)
            val currentSets = workouts.getSetsForSession(sessionId).filter { it.programExerciseId == pe.id }
            val alternates = workouts.getAlternates(ex.id)
            PlannedExerciseUi(pe, ex, currentSets, priorBest, suggestion, alternates)
        }
        _state.update {
            it.copy(
                loading = false,
                session = session,
                dayLabel = session.programDayLabelSnapshot,
                items = items,
                baselineWeekActive = baseline
            )
        }
    }

    /**
     * Swap a planned exercise to an alternate. If [saveToProgram] is true, persists the swap on
     * the ProgramExercise row so future sessions inherit it. Otherwise the swap is recorded in
     * this VM's override map and the slot's logged sets are retargeted to the new exercise
     * snapshot. Either way, [load] picks up the change on the next refresh.
     */
    fun swapExercise(programExercise: ProgramExercise, newExerciseId: Long, saveToProgram: Boolean) {
        viewModelScope.launch {
            if (saveToProgram) {
                workouts.persistExerciseSwap(programExercise, newExerciseId)
                sessionOverrides.remove(programExercise.id)
            } else {
                sessionOverrides[programExercise.id] = newExerciseId
            }
            workouts.retargetSetsForSlot(sessionId, programExercise.id, newExerciseId)
            load()
        }
    }

    fun logSet(
        programExercise: ProgramExercise,
        exercise: Exercise,
        setIndex: Int,
        weightKg: Double?,
        reps: Int?,
        rpe: Int?,
        isWarmup: Boolean = false
    ) {
        viewModelScope.launch {
            val userId = currentUserIdProvider.requireUserId()
            workouts.logSet(
                SetEntry(
                    userId = userId,
                    sessionId = sessionId,
                    programExerciseId = programExercise.id,
                    exerciseIdSnapshot = exercise.id,
                    exerciseNameSnapshot = exercise.name,
                    setIndex = setIndex,
                    weightKg = weightKg,
                    reps = reps,
                    rpe = rpe,
                    isWarmup = isWarmup,
                    completedAt = LocalDateTime.now()
                )
            )
            // Skip rest timer on warmup sets and on the very last set of the last exercise.
            val items = _state.value.items
            val itemIndex = items.indexOfFirst { it.programExercise.id == programExercise.id }
            val isLastSet = setIndex >= programExercise.sets
            val isLastExercise = itemIndex == items.size - 1
            if (!isWarmup && !(isLastSet && isLastExercise)) {
                RestTimerService.start(appContext, programExercise.restSec, exercise.name)
            }
            load()
        }
    }

    fun setKneeFeel(value: Int?) = _state.update { it.copy(kneeFeel = value) }
    fun setNotes(value: String) = _state.update { it.copy(notes = value) }

    fun finish() = viewModelScope.launch {
        workouts.completeSession(sessionId, _state.value.kneeFeel, _state.value.notes.ifBlank { null })
        RestTimerService.stop(appContext)
        _state.update { it.copy(finished = true) }
    }
}

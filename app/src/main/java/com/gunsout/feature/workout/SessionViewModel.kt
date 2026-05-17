package com.gunsout.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    private val engine = ProgressionEngine()
    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        val baseline = userPrefs.profile.first().baselineWeekActive
        val sessions = workouts.getRecentSessions()
        val session = sessions.firstOrNull { it.id == sessionId } ?: return@launch
        val pdId = session.programDayId ?: return@launch
        val pds = workouts.getProgramExercises(pdId)
        val items = pds.map { pe ->
            val ex = workouts.getExercise(pe.exerciseId)
                ?: Exercise(id = pe.exerciseId, name = "(unknown)",
                    primaryMuscleGroup = com.gunsout.data.entity.MuscleGroup.OTHER,
                    equipment = com.gunsout.data.entity.Equipment.OTHER)
            val recent = workouts.getPreviousSetsForExercise(ex.id)
            val previousBest = recent
                .filter { it.sessionId != sessionId }
                .groupBy { it.sessionId }
                .toSortedMap(compareByDescending { it })
                .values.firstOrNull()
                ?.maxByOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
            val previousSets = recent
                .filter { it.sessionId != sessionId }
                .groupBy { it.sessionId }
                .toSortedMap(compareByDescending { it })
                .values.firstOrNull().orEmpty()
            val suggestion = engine.suggest(pe, ex, previousSets, baseline)
            val currentSets = workouts.getSetsForSession(sessionId).filter { it.programExerciseId == pe.id }
            val alternates = workouts.getAlternates(ex.id)
            PlannedExerciseUi(pe, ex, currentSets, previousBest, suggestion, alternates)
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
     * the ProgramExercise row so future sessions inherit it. Otherwise the swap only applies to
     * the current session, by retargeting any already-logged sets for this slot to the new
     * exercise snapshot.
     */
    fun swapExercise(programExercise: ProgramExercise, newExerciseId: Long, saveToProgram: Boolean) {
        viewModelScope.launch {
            if (saveToProgram) {
                workouts.persistExerciseSwap(programExercise, newExerciseId)
            } else {
                // Retarget existing SetEntry rows for this slot in this session.
                workouts.retargetSetsForSlot(sessionId, programExercise.id, newExerciseId)
            }
            load()
        }
    }

    fun logSet(programExercise: ProgramExercise, exercise: Exercise, setIndex: Int, weightKg: Double?, reps: Int?, rpe: Int?) {
        viewModelScope.launch {
            workouts.logSet(
                SetEntry(
                    sessionId = sessionId,
                    programExerciseId = programExercise.id,
                    exerciseIdSnapshot = exercise.id,
                    exerciseNameSnapshot = exercise.name,
                    setIndex = setIndex,
                    weightKg = weightKg,
                    reps = reps,
                    rpe = rpe,
                    completedAt = LocalDateTime.now()
                )
            )
            RestTimerService.start(appContext, programExercise.restSec, exercise.name)
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

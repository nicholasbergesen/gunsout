package com.gunsout.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.auth.CurrentUserIdProvider
import com.gunsout.data.entity.Equipment
import com.gunsout.data.entity.Exercise
import com.gunsout.data.entity.MuscleGroup
import com.gunsout.data.entity.SetEntry
import com.gunsout.data.repo.ProgramRepository
import com.gunsout.data.repo.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryListViewModel @Inject constructor(
    private val repo: ProgramRepository,
    currentUserIdProvider: CurrentUserIdProvider
) : ViewModel() {
    val exercises: StateFlow<List<Exercise>> = currentUserIdProvider.currentUserId
        .filterNotNull()
        .flatMapLatest { userId -> repo.observeExercises(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class ExerciseEditState(
    val name: String = "",
    val muscle: MuscleGroup = MuscleGroup.CHEST,
    val equipment: Equipment = Equipment.DUMBBELL,
    val formNotes: String = "",
    val defaultRestSec: String = "90",
    val history: List<HistoryPoint> = emptyList(),
    val saved: Boolean = false
)

/** One data point per past session: top working-set weight for this exercise on that date. */
data class HistoryPoint(val sessionId: Long, val date: java.time.LocalDate, val topWeightKg: Double, val reps: Int)

@HiltViewModel
class ExerciseEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ProgramRepository,
    private val workoutRepo: WorkoutRepository,
    private val currentUserIdProvider: CurrentUserIdProvider
) : ViewModel() {
    private val exerciseId: Long = savedStateHandle.get<Long>("exerciseId") ?: 0L
    private val _state = MutableStateFlow(ExerciseEditState())
    val state: StateFlow<ExerciseEditState> = _state.asStateFlow()

    private var existing: Exercise? = null

    init {
        if (exerciseId > 0) viewModelScope.launch {
            val e = repo.getExercise(exerciseId) ?: return@launch
            existing = e
            val history = buildHistory(exerciseId)
            _state.value = ExerciseEditState(
                name = e.name, muscle = e.primaryMuscleGroup, equipment = e.equipment,
                formNotes = e.formNotes.orEmpty(),
                defaultRestSec = e.defaultRestSec.toString(),
                history = history
            )
        }
    }

    private suspend fun buildHistory(exerciseId: Long): List<HistoryPoint> {
        val userId = currentUserIdProvider.requireUserId()
        val recent: List<SetEntry> = workoutRepo.getPreviousSetsForExercise(userId, exerciseId)
        // Group by session, take the heaviest working-set per session, attach the session date.
        val sessionIds = recent.map { it.sessionId }.distinct()
        val sessionsById = sessionIds.mapNotNull { id ->
            workoutRepo.getSessionById(id)?.let { id to it }
        }.toMap()
        return recent
            .filter { !it.isWarmup && it.weightKg != null && it.reps != null }
            .groupBy { it.sessionId }
            .mapNotNull { (sid, sets) ->
                val top = sets.maxByOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 0) } ?: return@mapNotNull null
                val date = sessionsById[sid]?.date ?: return@mapNotNull null
                HistoryPoint(sid, date, top.weightKg ?: 0.0, top.reps ?: 0)
            }
            .sortedBy { it.date }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setMuscle(v: MuscleGroup) = _state.update { it.copy(muscle = v) }
    fun setEquipment(v: Equipment) = _state.update { it.copy(equipment = v) }
    fun setFormNotes(v: String) = _state.update { it.copy(formNotes = v) }
    fun setRestSec(v: String) = _state.update { it.copy(defaultRestSec = v.filter(Char::isDigit)) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.name.isBlank()) return@launch
        val userId = currentUserIdProvider.requireUserId()
        val ex = (existing ?: Exercise(
            userId = userId,
            name = "",
            primaryMuscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.DUMBBELL,
            isUserCreated = true
        )).copy(
            name = s.name.trim(),
            primaryMuscleGroup = s.muscle,
            equipment = s.equipment,
            formNotes = s.formNotes.ifBlank { null },
            defaultRestSec = s.defaultRestSec.toIntOrNull() ?: 90
        )
        if (ex.id > 0) repo.updateExercise(ex) else repo.createExercise(ex.copy(isUserCreated = true))
        _state.update { it.copy(saved = true) }
    }
}

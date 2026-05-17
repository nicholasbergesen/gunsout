package com.gunsout.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.data.entity.Equipment
import com.gunsout.data.entity.Exercise
import com.gunsout.data.entity.MuscleGroup
import com.gunsout.data.repo.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryListViewModel @Inject constructor(
    private val repo: ProgramRepository
) : ViewModel() {
    val exercises: StateFlow<List<Exercise>> = repo.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class ExerciseEditState(
    val name: String = "",
    val muscle: MuscleGroup = MuscleGroup.CHEST,
    val equipment: Equipment = Equipment.DUMBBELL,
    val formNotes: String = "",
    val defaultRestSec: String = "90",
    val saved: Boolean = false
)

@HiltViewModel
class ExerciseEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ProgramRepository
) : ViewModel() {
    private val exerciseId: Long = savedStateHandle.get<Long>("exerciseId") ?: 0L
    private val _state = MutableStateFlow(ExerciseEditState())
    val state: StateFlow<ExerciseEditState> = _state.asStateFlow()

    private var existing: Exercise? = null

    init {
        if (exerciseId > 0) viewModelScope.launch {
            val e = repo.getExercise(exerciseId) ?: return@launch
            existing = e
            _state.value = ExerciseEditState(
                name = e.name, muscle = e.primaryMuscleGroup, equipment = e.equipment,
                formNotes = e.formNotes.orEmpty(),
                defaultRestSec = e.defaultRestSec.toString()
            )
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setMuscle(v: MuscleGroup) = _state.update { it.copy(muscle = v) }
    fun setEquipment(v: Equipment) = _state.update { it.copy(equipment = v) }
    fun setFormNotes(v: String) = _state.update { it.copy(formNotes = v) }
    fun setRestSec(v: String) = _state.update { it.copy(defaultRestSec = v.filter(Char::isDigit)) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.name.isBlank()) return@launch
        val ex = (existing ?: Exercise(
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

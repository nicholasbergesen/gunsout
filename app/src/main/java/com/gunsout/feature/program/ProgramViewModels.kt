package com.gunsout.feature.program

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.auth.CurrentUserIdProvider
import com.gunsout.data.entity.Exercise
import com.gunsout.data.entity.Program
import com.gunsout.data.entity.ProgramDay
import com.gunsout.data.entity.ProgramExercise
import com.gunsout.data.repo.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgramListViewModel @Inject constructor(
    private val repo: ProgramRepository,
    private val currentUserIdProvider: CurrentUserIdProvider
) : ViewModel() {
    val programs: StateFlow<List<Program>> = currentUserIdProvider.currentUserId
        .filterNotNull()
        .flatMapLatest { userId -> repo.observePrograms(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun activate(id: Long) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        repo.setActive(userId, id)
    }
    fun duplicate(id: Long, name: String) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        repo.duplicateProgram(userId, id, name)
    }
    fun rename(id: Long, name: String) = viewModelScope.launch { repo.renameProgram(id, name) }
    fun delete(id: Long) = viewModelScope.launch { repo.deleteProgram(id) }
    fun createBlank(name: String, onCreated: (Long) -> Unit) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        val id = repo.createBlankProgram(userId, name)
        onCreated(id)
    }
}

data class ProgramEditState(
    val program: Program? = null,
    val days: List<ProgramDay> = emptyList(),
    val exercisesByDayId: Map<Long, List<ProgramExercise>> = emptyMap(),
    val exercisesById: Map<Long, Exercise> = emptyMap()
)

@HiltViewModel
class ProgramEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ProgramRepository,
    private val currentUserIdProvider: CurrentUserIdProvider
) : ViewModel() {
    private val programId: Long = savedStateHandle.get<Long>("programId") ?: 0L

    private val _state = MutableStateFlow(ProgramEditState())
    val state: StateFlow<ProgramEditState> = _state

    init { reload() }

    fun reload() = viewModelScope.launch {
        val program = repo.getProgram(programId)
        val days = if (program != null) {
            repo.observeDaysFor(program.id).firstOrNull().orEmpty()
        } else emptyList()
        val byDay = mutableMapOf<Long, List<ProgramExercise>>()
        val exerciseIds = mutableSetOf<Long>()
        for (d in days) {
            val list = repo.observeExercisesForDay(d.id).firstOrNull().orEmpty()
            byDay[d.id] = list
            exerciseIds += list.map { it.exerciseId }
        }
        val byExId = exerciseIds.mapNotNull { id -> repo.getExercise(id)?.let { id to it } }.toMap()
        _state.value = ProgramEditState(program, days, byDay, byExId)
    }

    fun addDay() = viewModelScope.launch {
        val p = _state.value.program ?: return@launch
        val userId = currentUserIdProvider.requireUserId()
        repo.addDay(userId, p.id, "Day ${_state.value.days.size + 1}")
        reload()
    }

    fun renameDay(day: ProgramDay, label: String) = viewModelScope.launch {
        repo.updateDay(day.copy(label = label))
        reload()
    }

    fun toggleDayRest(day: ProgramDay) = viewModelScope.launch {
        repo.updateDay(day.copy(isRest = !day.isRest))
        reload()
    }

    fun deleteDay(day: ProgramDay) = viewModelScope.launch {
        repo.deleteDay(day.id)
        reload()
    }

    fun addExerciseToDay(day: ProgramDay, exerciseId: Long) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        repo.addExerciseToDay(userId, day.id, exerciseId)
        reload()
    }

    fun updateProgramExercise(pe: ProgramExercise) = viewModelScope.launch {
        repo.updateProgramExercise(pe)
        reload()
    }

    fun deleteProgramExercise(pe: ProgramExercise) = viewModelScope.launch {
        repo.deleteProgramExercise(pe.id)
        reload()
    }
}

package com.gunsout.feature.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.data.entity.Exercise
import com.gunsout.data.repo.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    repo: ProgramRepository
) : ViewModel() {
    val exercises: StateFlow<List<Exercise>> = repo.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

package com.nicholasbergesen.gunsout.feature.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicholasbergesen.gunsout.auth.CurrentUserIdProvider
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.repo.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    repo: ProgramRepository,
    currentUserIdProvider: CurrentUserIdProvider
) : ViewModel() {
    val exercises: StateFlow<List<Exercise>> = currentUserIdProvider.currentUserId
        .filterNotNull()
        .flatMapLatest { userId -> repo.observeExercises(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

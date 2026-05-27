package com.gunsout.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.auth.CurrentUserIdProvider
import com.gunsout.data.entity.SessionStatus
import com.gunsout.data.entity.SetEntry
import com.gunsout.data.entity.WorkoutSession
import com.gunsout.data.repo.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryListViewModel @Inject constructor(
    private val workouts: WorkoutRepository,
    private val currentUserIdProvider: CurrentUserIdProvider
) : ViewModel() {

    val sessions: StateFlow<List<WorkoutSession>> = currentUserIdProvider.currentUserId
        .filterNotNull()
        .flatMapLatest { userId -> workouts.observeAllSessions(userId) }
        .map { list -> list.filter { it.status == SessionStatus.COMPLETED }.sortedByDescending { it.date } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class HistoryDetailState(
    val session: WorkoutSession? = null,
    val sets: List<SetEntry> = emptyList()
)

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workouts: WorkoutRepository
) : ViewModel() {
    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L
    private val _state = MutableStateFlow(HistoryDetailState())
    val state: StateFlow<HistoryDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val session = workouts.getSessionById(sessionId)
            val sets = if (session != null) workouts.getSetsForSession(sessionId) else emptyList()
            _state.value = HistoryDetailState(session, sets.sortedBy { it.setIndex })
        }
    }
}

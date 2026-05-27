package com.gunsout.auth

import com.gunsout.data.seed.Seeder
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives the per-user seeding step that runs between successful sign-in and
 * the first user-facing screen. [AuthGate] observes [state] to decide whether
 * to render the SetupScreen (while seeding) or the main app content (once
 * Done).
 *
 * Seeding for a given userId runs at most once per process; once the seeder
 * has reported Done for a userId, subsequent calls become idempotent no-ops.
 * If sign-out brings the user back to null and then a different userId signs
 * in, that new userId triggers a fresh seeding pass.
 */
@Singleton
class SeederController @Inject constructor(
    private val seeder: Seeder
) {
    private val _state = MutableStateFlow<SeederState>(SeederState.Idle)
    val state: StateFlow<SeederState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val seededUserIds = mutableSetOf<String>()

    /**
     * Kick off a seeding pass for [userId]. Safe to call repeatedly: the first
     * call wins and subsequent calls for the same userId are no-ops.
     */
    fun start(userId: String) {
        scope.launch {
            mutex.withLock {
                if (userId in seededUserIds) {
                    _state.value = SeederState.Done(userId)
                    return@withLock
                }
                _state.value = SeederState.Seeding(userId)
                try {
                    seeder.seedIfNeeded(userId)
                    seededUserIds += userId
                    _state.value = SeederState.Done(userId)
                } catch (t: Throwable) {
                    _state.value = SeederState.Failed(userId, t.message ?: "Seeding failed")
                }
            }
        }
    }

    /** Resets the controller back to Idle. Called when the user signs out. */
    fun reset() {
        _state.value = SeederState.Idle
    }
}

sealed interface SeederState {
    data object Idle : SeederState
    data class Seeding(val userId: String) : SeederState
    data class Done(val userId: String) : SeederState
    data class Failed(val userId: String, val message: String) : SeederState
}

package com.nicholasbergesen.gunsout.auth

import com.nicholasbergesen.gunsout.data.seed.Seeder
import com.nicholasbergesen.gunsout.feature.supplements.SupplementReminderScheduler
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
 *
 * After seeding completes successfully, supplement reminders are armed for
 * the user. This covers both initial sign-in and subsequent app launches
 * (where the seeder is a no-op because firstRunDone is set, but reminders
 * still need to be re-armed in case Android dropped pending alarms).
 */
@Singleton
class SeederController @Inject constructor(
    private val seeder: Seeder,
    private val reminderScheduler: SupplementReminderScheduler
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
                    // Same user signing back in within the same process. Seeding itself is a no-op
                    // (the per-user catalog already exists), but reminders must still be re-armed
                    // because the prior sign-out called reminderScheduler.cancelForUser(...) and
                    // tore down every PendingIntent for this user. Without this call the user
                    // would silently lose all supplement reminders on every sign-out / sign-in
                    // cycle even though the contract is "re-arm after sign-in".
                    runCatching { reminderScheduler.armForUser(userId) }
                    _state.value = SeederState.Done(userId)
                    return@withLock
                }
                _state.value = SeederState.Seeding(userId)
                try {
                    seeder.seedIfNeeded(userId)
                    runCatching { reminderScheduler.armForUser(userId) }
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


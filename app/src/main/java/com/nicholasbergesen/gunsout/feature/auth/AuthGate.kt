package com.nicholasbergesen.gunsout.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicholasbergesen.gunsout.auth.AuthRepository
import com.nicholasbergesen.gunsout.auth.AuthUser
import com.nicholasbergesen.gunsout.auth.SeederController
import com.nicholasbergesen.gunsout.auth.SeederState
import com.nicholasbergesen.gunsout.data.prefs.UserPreferences
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import com.nicholasbergesen.gunsout.feature.creatine.CreatineReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    val seederController: SeederController,
    private val userPreferences: UserPreferences,
    private val reminderScheduler: CreatineReminderScheduler
) : ViewModel() {
    val signedInUser: StateFlow<AuthUser?> = authRepository.signedInUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Re-runs seeding for [userId] after a [SeederState.Failed]. Safe to call from the
     * SetupErrorScreen retry button: failed users are not added to SeederController's
     * `seededUserIds` set, so [SeederController.start] runs the seed sequence again instead of
     * short-circuiting to Done. Successful seeds are idempotent thanks to the `getBySeedKey`
     * guards inside Seeder, and the seed body is wrapped in a Room transaction so a partial
     * failure rolls back cleanly between attempts.
     */
    fun retry(userId: String) {
        seederController.start(userId)
    }

    fun profile(userId: String): Flow<UserProfile> =
        userPreferences.profile(userId)

    /**
     * Sign-out path from the setup error screen. Reminder cancellation is best-effort: if it
     * throws (e.g. an AlarmManager exception or DAO failure inside cancelForUser), we must still
     * proceed with sign-out so the user has a way off the stuck setup screen.
     */
    fun signOut(leavingUserId: String?) = viewModelScope.launch {
        if (leavingUserId != null) {
            runCatching { reminderScheduler.cancelForUser(leavingUserId) }
        }
        authRepository.signOut()
    }
}

/**
 * Gates the application content behind successful sign-in and the per-user
 * seeder. While the user is signed out, renders [LoginScreen]; immediately
 * after a successful sign-in renders [SetupScreen] until the seeder reports
 * Done, then renders [content]. If seeding fails, renders [SetupErrorScreen]
 * with Retry and Sign-out actions so the user is never trapped on the spinner.
 */
@Composable
fun AuthGate(
    content: @Composable (userId: String) -> Unit
) {
    val vm: AuthGateViewModel = hiltViewModel()
    val user by vm.signedInUser.collectAsState()
    val seederState by vm.seederController.state.collectAsState()

    LaunchedEffect(user) {
        val u = user
        if (u != null) {
            vm.seederController.start(u.userId)
        } else {
            vm.seederController.reset()
        }
    }

    val currentUser = user
    if (currentUser == null) {
        LoginScreen()
        return
    }
    val profileFlow = remember(currentUser.userId) { vm.profile(currentUser.userId) }
    val profile by profileFlow.collectAsState(initial = UserProfile())
    val s = seederState
    when (s) {
        is SeederState.Done -> if (s.userId == currentUser.userId) {
            if (profile.profileSetupDone) {
                content(currentUser.userId)
            } else {
                ProfileSetupScreen()
            }
        } else {
            SetupScreen()
        }
        is SeederState.Failed -> SetupErrorScreen(
            message = s.message,
            onRetry = { vm.retry(currentUser.userId) },
            onSignOut = { vm.signOut(currentUser.userId) }
        )
        else -> SetupScreen()
    }
}

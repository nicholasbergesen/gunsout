package com.gunsout.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.auth.AuthRepository
import com.gunsout.auth.AuthUser
import com.gunsout.auth.SeederController
import com.gunsout.auth.SeederState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    authRepository: AuthRepository,
    val seederController: SeederController
) : ViewModel() {
    val signedInUser: StateFlow<AuthUser?> = authRepository.signedInUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/**
 * Gates the application content behind successful sign-in and the per-user
 * seeder. While the user is signed out, renders [LoginScreen]; immediately
 * after a successful sign-in renders [SetupScreen] until the seeder reports
 * Done, then renders [content].
 */
@Composable
fun AuthGate(
    content: @Composable (userId: String) -> Unit
) {
    // Use SimpleViewModel to access state without coupling to androidx.lifecycle.viewModelScope
    // (Hilt-provided LoginViewModel and SeederController are accessed via the gate VM below).
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
    val s = seederState
    when (s) {
        is SeederState.Done -> if (s.userId == currentUser.userId) {
            content(currentUser.userId)
        } else {
            SetupScreen()
        }
        is SeederState.Failed -> {
            // Render setup with a fallback message; future enhancement could
            // surface retry. For now, sign-out is the user's recovery path
            // (Settings → Sign out) and the SetupScreen at least communicates
            // that something is happening.
            SetupScreen()
        }
        else -> SetupScreen()
    }
}

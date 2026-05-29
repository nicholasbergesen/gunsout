package com.nicholasbergesen.gunsout.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Test double for [AuthRepository]. Lets tests drive [signedInUser] transitions
 * deterministically without going through Credential Manager.
 *
 * Constructed and held by tests directly (no Hilt @Inject); production code
 * keeps its own real AuthRepository binding.
 */
class FakeAuthRepository(initialUser: AuthUser? = null) {
    private val _signedInUser = MutableStateFlow(initialUser)
    val signedInUser: Flow<AuthUser?> = _signedInUser

    fun emitUser(user: AuthUser?) {
        _signedInUser.value = user
    }

    fun emitSignIn(userId: String, email: String? = null, displayName: String? = null) {
        _signedInUser.value = AuthUser(userId = userId, email = email, displayName = displayName)
    }

    fun emitSignOut() {
        _signedInUser.value = null
    }
}

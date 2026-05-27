package com.gunsout.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single read-side surface for "who is the current user?".
 *
 * Repositories that need to scope a query by user accept the [userId] as a
 * parameter; they never inject [CurrentUserIdProvider] directly. ViewModels
 * build user-scoped flows via [currentUserId] combined with `flatMapLatest`
 * so they react to sign-in/sign-out transitions without ever caching a stale
 * userId.
 */
@Singleton
class CurrentUserIdProvider @Inject constructor(
    private val sessionStore: AuthSessionStore
) {
    val currentUserId: Flow<String?> = sessionStore.currentSignedInUserId

    /**
     * Returns the currently signed-in userId or throws. Use only from code
     * paths guaranteed to run while the user is signed in (e.g. inside an
     * `AuthGate { ... }` content lambda or a feature ViewModel that observes
     * a non-null userId before kicking off any work).
     */
    suspend fun requireUserId(): String =
        currentUserId.first()
            ?: throw IllegalStateException("Required signed-in userId but no user is signed in")
}

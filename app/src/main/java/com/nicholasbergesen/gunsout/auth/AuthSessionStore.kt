package com.nicholasbergesen.gunsout.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authSessionStore: DataStore<Preferences> by preferencesDataStore(
    name = AuthSessionStore.FILE_NAME
)

/**
 * Persists the currently signed-in [AuthUser] across process restarts.
 *
 * The [currentSignedInUserId] flow is the source of truth that drives
 * AuthGate, BootReceiver re-arming, and any other "who is using the app right
 * now" decisions. The [lastSignedInEmail] flow is UX-only (e.g. pre-filling
 * the login screen) and must never be used as an identity for data access or
 * for re-arming reminders after sign-out.
 *
 * The backing DataStore file is excluded from Android Auto Backup; identity
 * never leaves the device.
 */
@Singleton
class AuthSessionStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val userId = stringPreferencesKey("current_signed_in_user_id")
        val email = stringPreferencesKey("current_signed_in_email")
        val displayName = stringPreferencesKey("current_signed_in_display_name")
        val lastEmail = stringPreferencesKey("last_signed_in_email")
    }

    val currentUser: Flow<AuthUser?> = context.authSessionStore.data.map { p ->
        val id = p[Keys.userId] ?: return@map null
        AuthUser(
            userId = id,
            email = p[Keys.email],
            displayName = p[Keys.displayName]
        )
    }

    val currentSignedInUserId: Flow<String?> = context.authSessionStore.data.map { p ->
        p[Keys.userId]
    }

    val lastSignedInEmail: Flow<String?> = context.authSessionStore.data.map { p ->
        p[Keys.lastEmail]
    }

    suspend fun set(user: AuthUser) {
        context.authSessionStore.edit { p ->
            p[Keys.userId] = user.userId
            user.email?.let { p[Keys.email] = it } ?: p.remove(Keys.email)
            user.displayName?.let { p[Keys.displayName] = it } ?: p.remove(Keys.displayName)
            user.email?.let { p[Keys.lastEmail] = it }
        }
    }

    suspend fun clear() {
        context.authSessionStore.edit { p ->
            p.remove(Keys.userId)
            p.remove(Keys.email)
            p.remove(Keys.displayName)
            // Intentionally keep Keys.lastEmail so the login screen can pre-fill or hint at the
            // last-known account on next sign-in.
        }
    }

    companion object {
        const val FILE_NAME = "auth_session"
    }
}

package com.gunsout

import android.app.Application
import com.gunsout.auth.AuthSessionStore
import com.gunsout.auth.AuthUser
import com.gunsout.data.seed.Seeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GunsoutApplication : Application() {

    @Inject lateinit var seeder: Seeder
    @Inject lateinit var authSessionStore: AuthSessionStore

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // Phase 2b-2 placeholder: until the AuthGate + LoginScreen ship in Phase 3, every
            // launch acts as a single anonymous user so the data layer (now userId-scoped) keeps
            // working. Phase 3 replaces this with a real Google Sign-In flow.
            val existing = authSessionStore.currentSignedInUserId.first()
            val userId = existing ?: run {
                authSessionStore.set(AuthUser(userId = ANONYMOUS_USER_ID, email = null, displayName = null))
                ANONYMOUS_USER_ID
            }
            seeder.seedIfNeeded(userId)
        }
    }

    companion object {
        const val ANONYMOUS_USER_ID = "anonymous"
    }
}

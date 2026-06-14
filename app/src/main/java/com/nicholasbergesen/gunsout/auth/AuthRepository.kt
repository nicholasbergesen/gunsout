package com.nicholasbergesen.gunsout.auth

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.nicholasbergesen.gunsout.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates Google Sign-In via Credential Manager and persists the resulting
 * [AuthUser] in [AuthSessionStore].
 *
 * Identity = the Google account's `sub` claim, decoded from the ID token JWT.
 * `sub` is stable across email changes and is the value used as `userId` on
 * every Room row and DataStore file. We deliberately avoid using
 * [GoogleIdTokenCredential.id] (which is the user's email) as identity so that
 * a user who changes their account email does not orphan their on-device data.
 *
 * Note on JWT parsing: the `androidx.credentials:googleid:1.1.1` library
 * exposes only `id` (email) and `idToken` (JWT) on [GoogleIdTokenCredential];
 * there is no `uniqueId` property in this release that would surface `sub`
 * directly. The lightweight [extractSubClaim] helper below decodes the
 * unsigned payload only - signature verification is not needed on-device for
 * an account-disambiguation use case (the ID token already came from a
 * trusted Credential Manager flow on the same device). Unit tests live in
 * `AuthRepositoryTest`.
 *
 * The repository is the only component that talks to Credential Manager; the
 * rest of the app reads [signedInUser] and reacts.
 */
@Singleton
class AuthRepository @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val sessionStore: AuthSessionStore
) {
    val signedInUser: Flow<AuthUser?> = sessionStore.currentUser

    /**
     * Triggers the Credential Manager UI to pick a Google account and signs
     * the resulting user in.
     *
     * @param activityContext must be an Activity context; Credential Manager
     *   needs it to attach the picker.
     */
    @SuppressLint("CredentialManagerSignInWithGoogle")
    suspend fun signIn(activityContext: Context): SignInResult {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            return SignInResult.ConfigurationError(
                "Google Sign-In is not configured for this build. " +
                    "Set GOOGLE_WEB_CLIENT_ID in local.properties or as a CI secret."
            )
        }
        val credentialManager = CredentialManager.create(activityContext)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        return try {
            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return SignInResult.UnknownError("Unexpected credential type: ${credential::class.java.simpleName}")
            }
            val googleCred = try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (e: GoogleIdTokenParsingException) {
                return SignInResult.UnknownError("Could not parse Google ID token: ${e.message}")
            }
            val sub = extractSubClaim(googleCred.idToken)
                ?: return SignInResult.UnknownError("Google ID token did not contain a sub claim")
            val authUser = AuthUser(
                userId = sub,
                email = googleCred.id,
                displayName = googleCred.displayName
            )
            sessionStore.set(authUser)
            SignInResult.Success(authUser)
        } catch (e: NoCredentialException) {
            SignInResult.NoGoogleAccount(
                "No Google account is available on this device. Add one in Settings, then try again."
            )
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: GetCredentialException) {
            SignInResult.UnknownError(e.message ?: "Sign-in failed")
        }
    }

    /**
     * Clears the active session and asks Credential Manager to forget the
     * previous selection so the next sign-in re-prompts.
     *
     * Callers should arrange to cancel any user-scoped reminders before
     * invoking this so the cancellation can read the leaving userId.
     */
    suspend fun signOut() {
        sessionStore.clear()
        try {
            val cm = CredentialManager.create(appContext)
            cm.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // ClearCredentialState is best-effort; the session is already cleared either way.
        }
    }

    sealed interface SignInResult {
        data class Success(val user: AuthUser) : SignInResult
        data object Cancelled : SignInResult
        data class NoGoogleAccount(val message: String) : SignInResult
        data class ConfigurationError(val message: String) : SignInResult
        data class UnknownError(val message: String) : SignInResult
    }

    companion object {
        private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

        internal fun extractSubClaim(idToken: String): String? {
            val parts = idToken.split('.')
            if (parts.size < 2) return null
            val payloadBytes = try {
                Base64.getUrlDecoder().decode(addBase64Padding(parts[1]))
            } catch (_: IllegalArgumentException) {
                return null
            }
            val payload = String(payloadBytes, Charsets.UTF_8)
            val sub = try {
                lenientJson.parseToJsonElement(payload).jsonObject["sub"]?.jsonPrimitive?.contentOrNull
            } catch (_: Exception) {
                return null
            }
            return sub?.takeIf { it.isNotEmpty() }
        }

        private fun addBase64Padding(s: String): String {
            val remainder = s.length % 4
            if (remainder == 0) return s
            return s + "=".repeat(4 - remainder)
        }
    }
}

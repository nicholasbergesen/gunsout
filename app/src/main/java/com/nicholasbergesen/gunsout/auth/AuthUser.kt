package com.nicholasbergesen.gunsout.auth

/**
 * Stable identity for a signed-in Google account.
 *
 * [userId] is the Google account's `sub` claim, which AuthRepository extracts
 * from `GoogleIdTokenCredential.idToken` (the JWT) because the
 * `androidx.credentials:googleid:1.1.1` library does not surface `sub`
 * directly as a credential property. We use `sub` rather than the email so
 * that a user changing the email address on their Google account does not
 * orphan their on-device data. The email is kept around for display only.
 */
data class AuthUser(
    val userId: String,
    val email: String?,
    val displayName: String?
)

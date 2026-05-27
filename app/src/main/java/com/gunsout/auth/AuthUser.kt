package com.gunsout.auth

/**
 * Stable identity for a signed-in Google account.
 *
 * [userId] is the Google account's `sub` claim, surfaced through
 * `GoogleIdTokenCredential.uniqueId`. We use this rather than the email so that
 * a user changing the email address on their Google account does not orphan
 * their on-device data. The email is kept around for display only.
 */
data class AuthUser(
    val userId: String,
    val email: String?,
    val displayName: String?
)

package com.gunsout.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies that [AuthRepository.extractSubClaim] picks the `sub` claim out of
 * a Google ID token JWT. The androidx.credentials:googleid:1.1.1 library does
 * not expose `sub` as a credential property, so AuthRepository decodes it
 * client-side from the ID token. We don't have a real Google JWT in unit
 * tests, so we build a syntactically valid one (header.payload.signature) and
 * confirm only the payload parsing is exercised.
 */
class AuthRepositoryTest {

    @Test
    fun `extractSubClaim reads sub from token payload`() {
        val token = buildToken("""{"sub":"1234567890","email":"user@example.com"}""")

        val sub = AuthRepository.extractSubClaim(token)

        assertEquals("1234567890", sub)
    }

    @Test
    fun `extractSubClaim returns null when sub is missing`() {
        val token = buildToken("""{"email":"user@example.com"}""")
        assertNull(AuthRepository.extractSubClaim(token))
    }

    @Test
    fun `extractSubClaim returns null for malformed token`() {
        assertNull(AuthRepository.extractSubClaim("not-a-jwt"))
        assertNull(AuthRepository.extractSubClaim(""))
    }

    private fun buildToken(payloadJson: String): String {
        val header = base64UrlNoPad("""{"alg":"none","typ":"JWT"}""".toByteArray(Charsets.UTF_8))
        val payload = base64UrlNoPad(payloadJson.toByteArray(Charsets.UTF_8))
        val signature = base64UrlNoPad("sig".toByteArray(Charsets.UTF_8))
        return "$header.$payload.$signature"
    }

    private fun base64UrlNoPad(bytes: ByteArray): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

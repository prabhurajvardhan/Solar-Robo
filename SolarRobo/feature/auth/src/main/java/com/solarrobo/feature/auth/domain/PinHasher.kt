package com.solarrobo.feature.auth.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject

/** PINs are never retained; only a salted, expensive verifier is returned. */
class PinHasher @Inject constructor() {
    private val random = SecureRandom()

    fun create(pin: String): PinVerifier {
        require(pin.matches(PIN_PATTERN)) { "PIN must contain 6 to 12 digits." }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val derived = derive(pin, salt)
        return try {
            PinVerifier(encode(salt), encode(derived))
        } finally {
            derived.fill(0)
        }
    }

    fun verify(pin: String, saltBase64: String, expectedBase64: String): Boolean {
        if (!pin.matches(PIN_PATTERN)) return false
        val salt = runCatching { Base64.getDecoder().decode(saltBase64) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(expectedBase64) }.getOrNull() ?: return false
        if (salt.size != SALT_BYTES || expected.size != OUTPUT_BYTES) return false
        val actual = derive(pin, salt)
        return try {
            MessageDigest.isEqual(actual, expected)
        } finally {
            actual.fill(0)
            expected.fill(0)
        }
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val chars = pin.toCharArray()
        val spec = PBEKeySpec(chars, salt, ITERATIONS, OUTPUT_BYTES * 8)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    companion object {
        const val ITERATIONS = 120_000
        const val OUTPUT_BYTES = 32
        const val SALT_BYTES = 16
        private val PIN_PATTERN = Regex("^[0-9]{6,12}$")
    }
}

data class PinVerifier(val saltBase64: String, val hashBase64: String)

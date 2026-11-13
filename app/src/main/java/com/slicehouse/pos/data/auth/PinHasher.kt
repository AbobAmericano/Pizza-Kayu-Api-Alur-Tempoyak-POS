package com.slicehouse.pos.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Turns PINs and recovery codes into one-way hashes, so the real values are
 * never stored. A 4-digit PIN has only 10,000 possibilities, so the slow,
 * salted PBKDF2 hash mostly stops someone with a copy of the database from
 * reading PINs at a glance; the real protection against guessing at the
 * counter is the lockout after 5 wrong tries.
 *
 * Stored format: `pbkdf2-sha256$<iterations>$<salt base64>$<hash base64>`.
 */
object PinHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PREFIX = "pbkdf2-sha256"
    private const val ITERATIONS = 60_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    /** Letters and digits that can't be confused when copied by hand (no 0/O, 1/I/L). */
    private const val CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val RECOVERY_CODE_LENGTH = 12

    private val random = SecureRandom()

    fun hash(secret: String, iterations: Int = ITERATIONS): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val derived = derive(secret, salt, iterations)
        val b64 = Base64.getEncoder()
        return listOf(PREFIX, iterations.toString(), b64.encodeToString(salt), b64.encodeToString(derived))
            .joinToString("$")
    }

    fun verify(secret: String, stored: String): Boolean {
        val parts = stored.split("$")
        if (parts.size != 4 || parts[0] != PREFIX) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        return runCatching {
            val b64 = Base64.getDecoder()
            val expected = b64.decode(parts[3])
            MessageDigest.isEqual(expected, derive(secret, b64.decode(parts[2]), iterations))
        }.getOrDefault(false)
    }

    /** PINs are 4 to 6 digits. */
    fun isValidPin(pin: String): Boolean = pin.length in 4..6 && pin.all { it in '0'..'9' }

    /** A new 12-character recovery code, shown to the owner once. */
    fun newRecoveryCode(): String =
        (1..RECOVERY_CODE_LENGTH).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")

    /** "ABCD-EFGH-JKMN" for writing down. */
    fun formatRecoveryCode(code: String): String = code.chunked(4).joinToString("-")

    /** Accepts what the owner types: any case, with or without dashes and spaces. */
    fun normalizeRecoveryCode(input: String): String =
        input.uppercase().filter { it.isLetterOrDigit() }

    private fun derive(secret: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}

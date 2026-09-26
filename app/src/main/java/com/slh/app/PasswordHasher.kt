package com.slh.app

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/*
 * ================================================================
 * PASSWORD HASHER
 * ================================================================
 *
 * Passwords used to be stored and compared as plain text (see
 * LoginScreen.kt's old `it.password == password` checks). That
 * meant anyone who could read the (otherwise encrypted-at-rest)
 * app data file could read every user's real password directly.
 *
 * This hashes passwords with PBKDF2WithHmacSHA256 + a random
 * per-password salt before they are ever stored.
 *
 * BACKWARD COMPATIBILITY:
 * Accounts created before this change still have their old plain
 * text password saved. [verify] transparently accepts either
 * format so nobody gets locked out — a legacy plaintext match
 * still logs in. Screens that *set* a password (Student/Teacher/
 * Admin management) should call [hash] before saving, and login
 * screens should call [verify] instead of comparing strings
 * directly. Once a password is changed through the app it is
 * saved hashed from then on.
 * ================================================================
 */
object PasswordHasher {

    private const val ITERATIONS = 10000
    private const val KEY_LENGTH_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    // Marks a value as one of our hashes, vs. legacy plaintext.
    private const val PREFIX = "pbkdf2$"

    /**
     * Hashes [password] with a fresh random salt.
     * Store the returned string in place of the raw password.
     */
    fun hash(password: String): String {

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val digest = pbkdf2(password, salt)

        return PREFIX +
                Base64.encodeToString(salt, Base64.NO_WRAP) +
                "$" +
                Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    /**
     * True if [stored] was produced by [hash]. False for a
     * legacy plaintext value.
     */
    fun isHashed(stored: String): Boolean {
        return stored.startsWith(PREFIX)
    }

    /**
     * Checks [candidatePassword] (what the user just typed)
     * against [stored] (whatever is saved for that account —
     * a PBKDF2 hash, or a legacy plaintext password).
     */
    fun verify(
        candidatePassword: String,
        stored: String
    ): Boolean {

        if (!isHashed(stored)) {

            // Legacy record: still plain text.
            return stored == candidatePassword
        }

        return try {

            val body =
                stored.removePrefix(PREFIX)

            val parts =
                body.split("$")

            if (parts.size != 2) {
                return false
            }

            val salt =
                Base64.decode(parts[0], Base64.NO_WRAP)

            val expected =
                Base64.decode(parts[1], Base64.NO_WRAP)

            val actual =
                pbkdf2(candidatePassword, salt)

            expected.contentEquals(actual)

        } catch (e: Exception) {

            false
        }
    }

    private fun pbkdf2(
        password: String,
        salt: ByteArray
    ): ByteArray {

        val spec =
            PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH_BITS
            )

        val factory =
            SecretKeyFactory.getInstance(ALGORITHM)

        return factory
            .generateSecret(spec)
            .encoded
    }
}

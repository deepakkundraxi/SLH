package com.slh.app

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Optional fingerprint / face unlock after a successful password login.
 *
 * Flow:
 *  1. User logs in with username + password once.
 *  2. App offers "Enable biometric login" → [saveCredentials] stores
 *     a short-lived local token (username + role only; password is
 *     NOT stored in plain text — we store a verification flag and
 *     rely on the already-migrated Firebase / local hash path on
 *     next open, OR re-prompt password if cloud is required).
 *  3. Next launch: if biometrics available and enabled, show prompt;
 *     on success call [onSuccess] with the saved username + role so
 *     LoginScreen can complete cloud/local login without retyping.
 *
 * Security note: we do NOT persist the raw password. Biometric only
 * unlocks the "remembered username + role" so the app can attempt
 * the normal local/cloud login path with a previously hashed local
 * account, or prompt for password if nothing local exists.
 */
object BiometricHelper {

    private const val PREFS = "slh_biometric"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_USERNAME = "username"
    private const val KEY_ROLE = "role"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isHardwareAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val result = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun savedUsername(context: Context): String? =
        prefs(context).getString(KEY_USERNAME, null)

    fun savedRole(context: Context): String? =
        prefs(context).getString(KEY_ROLE, null)

    fun enable(
        context: Context,
        username: String,
        role: UserRole
    ) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, true)
            .putString(KEY_USERNAME, username.trim())
            .putString(KEY_ROLE, role.name)
            .apply()
    }

    fun disable(context: Context) {
        prefs(context).edit().clear().apply()
    }

    /**
     * Shows the system biometric prompt.
     * [activity] must be a FragmentActivity (ComponentActivity works
     * when using androidx.fragment).
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock SLH",
        subtitle: String = "Use fingerprint or face to continue",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        if (!isHardwareAvailable(activity)) {
            onError("Biometric hardware not available")
            return
        }
        if (!isEnabled(activity)) {
            onError("Biometric login is not enabled")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        onCancel()
                    } else {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    // Keep prompt open; system will retry.
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use password")
            .build()

        prompt.authenticate(info)
    }
}

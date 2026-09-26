package com.slh.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Auto-logout after a period of inactivity.
 *
 * Call [touch] on any user interaction (or from a periodic
 * LaunchedEffect while a screen is visible). Call [checkExpired]
 * before navigating / rendering protected content; if true, force
 * logout and return to the login screen.
 *
 * Default timeout: 30 minutes. Principal is not exempt — change
 * [timeoutMs] if you want a different policy.
 */
object SessionManager {

    private const val PREFS = "slh_session"
    private const val KEY_LAST_ACTIVE = "last_active_ms"
    private const val KEY_ENABLED = "timeout_enabled"

    /** 30 minutes */
    var timeoutMs: Long = 30L * 60L * 1000L

    var enabled by mutableStateOf(true)
        private set

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        enabled = prefs?.getBoolean(KEY_ENABLED, true) ?: true
        if (lastActiveMs() == 0L) {
            touch()
        }
    }

    /** Prefer this over assigning [enabled] directly — also persists. */
    fun updateEnabled(value: Boolean) {
        enabled = value
        prefs?.edit()?.putBoolean(KEY_ENABLED, value)?.apply()
        if (value) touch()
    }

    /** Record that the user is still active. */
    fun touch() {
        prefs?.edit()
            ?.putLong(KEY_LAST_ACTIVE, System.currentTimeMillis())
            ?.apply()
    }

    fun lastActiveMs(): Long =
        prefs?.getLong(KEY_LAST_ACTIVE, 0L) ?: 0L

    /**
     * Returns true when the session should be considered expired
     * (caller must log the user out and go to login).
     */
    fun checkExpired(): Boolean {
        if (!enabled) return false
        val last = lastActiveMs()
        if (last == 0L) return false
        return System.currentTimeMillis() - last > timeoutMs
    }

    fun clear() {
        prefs?.edit()?.remove(KEY_LAST_ACTIVE)?.apply()
    }

    /** Remaining ms before timeout, or null if disabled / not started. */
    fun remainingMs(): Long? {
        if (!enabled) return null
        val last = lastActiveMs()
        if (last == 0L) return null
        val left = timeoutMs - (System.currentTimeMillis() - last)
        return left.coerceAtLeast(0L)
    }
}
package com.slh.app

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private const val PREFS_NAME = "slh_prefs"
private const val KEY_THEME_MODE = "theme_mode" // "system" | "light" | "dark"

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Single shared source of truth for the app's theme mode.
 *
 * Backed by a Compose [androidx.compose.runtime.State], so every
 * screen that reads [ThemePreference.current] (directly, or via
 * [rememberThemeMode]) recomposes immediately when any other
 * screen changes it — no app restart needed, and no risk of one
 * screen's toggle being invisible to the rest of the app.
 */
object ThemePreference {

    var current: ThemeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    private var initialized = false

    /**
     * Call once, before Compose content is set (e.g. in
     * MainActivity.onCreate), to load the saved preference.
     */
    fun init(context: Context) {

        if (initialized) {
            return
        }

        initialized = true

        current = readFromPrefs(context)
    }

    private fun readFromPrefs(context: Context): ThemeMode {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        return when (
            prefs.getString(
                KEY_THEME_MODE,
                "system"
            )
        ) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    fun set(context: Context, mode: ThemeMode) {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        prefs.edit()
            .putString(
                KEY_THEME_MODE,
                when (mode) {
                    ThemeMode.LIGHT -> "light"
                    ThemeMode.DARK -> "dark"
                    ThemeMode.SYSTEM -> "system"
                }
            )
            .apply()

        current = mode
    }
}

/**
 * Reads/writes the single shared [ThemePreference.current].
 *
 * Every call site (MainActivity, Admin/Teacher/Principal/Student
 * screens) observes the SAME state, so changing it anywhere
 * updates the whole app immediately.
 */
@Composable
fun rememberThemeMode(): Pair<ThemeMode, (ThemeMode) -> Unit> {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    val setMode: (ThemeMode) -> Unit = { newMode ->
        ThemePreference.set(context, newMode)
    }

    return ThemePreference.current to setMode
}
/**
 * True when the APP is currently showing a dark UI —
 * respects Light / Dark / System preference, not only the
 * phone's system setting. Use this for theme-aware logos.
 */
@Composable
fun isAppInDarkTheme(): Boolean {
    return when (ThemePreference.current) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
}

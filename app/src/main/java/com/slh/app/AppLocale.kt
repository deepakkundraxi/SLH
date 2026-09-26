package com.slh.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Simple in-app language switch (English / Hindi).
 * Does not restart the activity — UI that reads [AppStrings]
 * recomposes when [language] changes.
 */
enum class AppLanguage {
    ENGLISH,
    HINDI
}

object AppLocale {

    private const val PREFS = "slh_locale"
    private const val KEY = "lang"

    var language by mutableStateOf(AppLanguage.ENGLISH)
        private set

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs?.getString(KEY, AppLanguage.ENGLISH.name)
        language = try {
            AppLanguage.valueOf(stored ?: AppLanguage.ENGLISH.name)
        } catch (_: Exception) {
            AppLanguage.ENGLISH
        }
    }

    /** Prefer this over assigning [language] directly — also persists. */
    fun updateLanguage(lang: AppLanguage) {
        language = lang
        prefs?.edit()?.putString(KEY, lang.name)?.apply()
    }

    fun toggle() {
        updateLanguage(
            if (language == AppLanguage.ENGLISH) {
                AppLanguage.HINDI
            } else {
                AppLanguage.ENGLISH
            }
        )
    }
}

/**
 * Common UI strings. Screens can use these instead of hard-coded
 * English where Hindi support is needed.
 */
object AppStrings {

    private val isHi: Boolean
        get() = AppLocale.language == AppLanguage.HINDI

    val appName: String
        get() = if (isHi) "SLH कोचिंग" else "SLH Coaching"

    val login: String
        get() = if (isHi) "लॉगिन" else "Login"

    val username: String
        get() = if (isHi) "यूज़रनेम" else "Username"

    val password: String
        get() = if (isHi) "पासवर्ड" else "Password"

    val students: String
        get() = if (isHi) "छात्र" else "Students"

    val teachers: String
        get() = if (isHi) "शिक्षक" else "Teachers"

    val batches: String
        get() = if (isHi) "बैच" else "Batches"

    val fees: String
        get() = if (isHi) "फीस" else "Fees"

    val attendance: String
        get() = if (isHi) "हाज़िरी" else "Attendance"

    val notices: String
        get() = if (isHi) "सूचनाएँ" else "Notices"

    val tests: String
        get() = if (isHi) "टेस्ट" else "Tests"

    val profile: String
        get() = if (isHi) "प्रोफ़ाइल" else "Profile"

    val logout: String
        get() = if (isHi) "लॉग आउट" else "Logout"

    val searchPlaceholder: String
        get() = if (isHi) "नाम, आईडी, मोबाइल खोजें…" else "Search name, id, mobile…"

    val exportData: String
        get() = if (isHi) "डेटा निर्यात" else "Export data"

    val exportStudents: String
        get() = if (isHi) "छात्र CSV" else "Students CSV"

    val exportFees: String
        get() = if (isHi) "फीस CSV" else "Fees CSV"

    val exportAttendance: String
        get() = if (isHi) "हाज़िरी CSV" else "Attendance CSV"

    val deleteAccount: String
        get() = if (isHi) "अकाउंट हटाएँ" else "Delete account"

    val deleteAccountConfirm: String
        get() = if (isHi)
            "क्या आप वाकई अपना अकाउंट हटाना चाहते हैं? यह क्रिया वापस नहीं हो सकती।"
        else
            "Are you sure you want to delete your account? This cannot be undone."

    val maintenanceTitle: String
        get() = if (isHi) "प्लेटफ़ॉर्म मेंटेनेंस में है" else "Platform under maintenance"

    val maintenanceBody: String
        get() = if (isHi)
            "अभी केवल प्रिंसिपल एडमिन लॉगिन कर सकते हैं। कृपया बाद में प्रयास करें।"
        else
            "Only Principal Admin login works right now. Please try again later."

    val sessionExpired: String
        get() = if (isHi)
            "सत्र समाप्त हो गया। कृपया फिर से लॉगिन करें।"
        else
            "Session expired. Please log in again."

    val enableBiometric: String
        get() = if (isHi) "फ़िंगरप्रिंट / फेस अनलॉक चालू करें" else "Enable fingerprint / face unlock"

    val useBiometric: String
        get() = if (isHi) "बायोमेट्रिक से लॉगिन" else "Login with biometric"

    val language: String
        get() = if (isHi) "भाषा" else "Language"

    val lastUpdated: String
        get() = if (isHi) "अंतिम अपडेट" else "Last updated"
}
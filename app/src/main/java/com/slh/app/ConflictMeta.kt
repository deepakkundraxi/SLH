package com.slh.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Lightweight conflict / freshness helpers.
 *
 * Local-first stores still use last-write-wins on cloud sync.
 * These helpers make "who changed what and when" visible in the UI
 * and keep a consistent ISO timestamp for future merge logic.
 */
object ConflictMeta {

    private val isoFormat = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        Locale.US
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val displayFormat = SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    )

    fun nowIso(): String = isoFormat.format(Date())

    fun formatForDisplay(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return try {
            val date = isoFormat.parse(iso) ?: return iso
            displayFormat.format(date)
        } catch (_: Exception) {
            iso
        }
    }

    /**
     * Returns true if [remoteIso] is strictly newer than [localIso].
     * Missing timestamps are treated as older.
     */
    fun isRemoteNewer(localIso: String?, remoteIso: String?): Boolean {
        if (remoteIso.isNullOrBlank()) return false
        if (localIso.isNullOrBlank()) return true
        return try {
            val local = isoFormat.parse(localIso)?.time ?: 0L
            val remote = isoFormat.parse(remoteIso)?.time ?: 0L
            remote > local
        } catch (_: Exception) {
            false
        }
    }
}

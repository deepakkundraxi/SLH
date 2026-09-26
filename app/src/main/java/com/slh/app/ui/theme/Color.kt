package com.slh.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------- Night / Dark theme ----------
val NightBackground = Color(0xFF0D1117)      // app background (deep charcoal-navy, not pure black -> less eye strain)
val NightSurface = Color(0xFF161B22)         // cards, dashboard tiles
val NightSurfaceHigh = Color(0xFF1F2733)     // elevated cards, dialogs, bottom sheets

val NightPrimary = Color(0xFF7C6FF0)         // Indigo - primary brand / active nav / buttons
val NightOnPrimary = Color(0xFFFFFFFF)
val NightPrimaryContainer = Color(0xFF2A2550)

val NightSecondary = Color(0xFF2DD4BF)       // Teal - success states, "present", fee paid
val NightOnSecondary = Color(0xFF00332E)

val NightTertiary = Color(0xFFFBBF24)        // Amber - notices, fee due, highlights
val NightOnTertiary = Color(0xFF3A2A00)

val NightError = Color(0xFFF87171)           // Red - absent, overdue, failed test
val NightOnError = Color(0xFF3A0A0A)

val NightTextPrimary = Color(0xFFE6EDF3)     // main text on dark bg
val NightTextSecondary = Color(0xFF8B949E)   // muted/secondary text
val NightBorder = Color(0xFF30363D)          // dividers, card outlines

// ---------- Light theme (kept clean/professional to pair with the night theme) ----------
val DayBackground = Color(0xFFF7F8FA)
val DaySurface = Color(0xFFFFFFFF)
val DaySurfaceHigh = Color(0xFFF1F0FE)

val DayPrimary = Color(0xFF5B4FE0)
val DayOnPrimary = Color(0xFFFFFFFF)
val DayPrimaryContainer = Color(0xFFE4E0FF)

val DaySecondary = Color(0xFF0F9D8C)
val DayOnSecondary = Color(0xFFFFFFFF)

val DayTertiary = Color(0xFFB6780A)
val DayOnTertiary = Color(0xFFFFFFFF)

val DayError = Color(0xFFD92D20)
val DayOnError = Color(0xFFFFFFFF)

val DayTextPrimary = Color(0xFF1C1F24)
val DayTextSecondary = Color(0xFF5B6472)
val DayBorder = Color(0xFFE3E5E8)

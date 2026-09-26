package com.slh.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SLH reusable colorful UI palette.
 *
 * Used across dashboards and management screens.
 */
object ChipColors {

    val blue = ColorPair(
        container = Color(0xFFE3F2FD),
        icon = Color(0xFF1565C0)
    )

    val green = ColorPair(
        container = Color(0xFFE8F5E9),
        icon = Color(0xFF2E7D32)
    )

    val purple = ColorPair(
        container = Color(0xFFF3E5F5),
        icon = Color(0xFF7B1FA2)
    )

    val amber = ColorPair(
        container = Color(0xFFFFF8E1),
        icon = Color(0xFFF57F17)
    )

    val red = ColorPair(
        container = Color(0xFFFFEBEE),
        icon = Color(0xFFC62828)
    )

    val orange = ColorPair(
        container = Color(0xFFFFF3E0),
        icon = Color(0xFFE65100)
    )

    val teal = ColorPair(
        container = Color(0xFFE0F2F1),
        icon = Color(0xFF00796B)
    )

    val pink = ColorPair(
        container = Color(0xFFFCE4EC),
        icon = Color(0xFFC2185B)
    )

    val indigo = ColorPair(
        container = Color(0xFFE8EAF6),
        icon = Color(0xFF3949AB)
    )

    data class ColorPair(
        val container: Color,
        val icon: Color
    )
}
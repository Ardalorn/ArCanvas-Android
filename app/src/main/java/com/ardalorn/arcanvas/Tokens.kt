package com.ardalorn.arcanvas

import androidx.compose.ui.graphics.Color

/**
 * Material You (dynamic colour) is always on where the device supports it (Android 12+). There is no
 * settings screen, so this is a build-time switch, not a user setting.
 */
const val DYNAMIC_COLOR = true

/**
 * Fallback palette for Android 11 and older, carried over from the web version's boilerplate-AA light
 * theme. [ArCanvasTheme] maps it onto Material 3 roles. On Android 12+ these are not used.
 */
object Tokens {
    val Paper = Color(0xFFEEF2EF)
    val Paper2 = Color(0xFFDEE7E2)
    val Ink = Color(0xFF12221E)
    val Soft = Color(0xFF3A4D47)
    val Edge = Color(0xFF5B736A)
}

data class Swatch(val name: String, val color: Color?)

/**
 * First swatch is the theme ink (a null [Swatch.color], resolved from the Material You palette when drawn);
 * the rest are the fixed pastels from the web version.
 */
val Swatches =
    listOf(
        Swatch("Ink", null),
        Swatch("Sage", Color(0xFF8FA882)),
        Swatch("Slate", Color(0xFF7B91A3)),
        Swatch("Terracotta", Color(0xFFC98B7A)),
        Swatch("Ochre", Color(0xFFD1B47E)),
        Swatch("Lavender", Color(0xFFA599B5)),
    )

data class BrushSize(val name: String, val strokeDp: Float, val dotDp: Float, val stampDp: Float)

val BrushSizes =
    listOf(
        BrushSize("small", strokeDp = 6f, dotDp = 6f, stampDp = 40f),
        BrushSize("medium", strokeDp = 16f, dotDp = 14f, stampDp = 64f),
        BrushSize("large", strokeDp = 32f, dotDp = 24f, stampDp = 92f),
        BrushSize("extra large", strokeDp = 48f, dotDp = 36f, stampDp = 130f),
    )

const val DEFAULT_SIZE_INDEX = 1

/** How long the Focus-mode island stays visible after the last touch. */
const val ISLAND_IDLE_MS = 3_000L

/** After this long the island starts pulsing as a gentle session reminder. */
const val SESSION_REMINDER_MS = 600_000L

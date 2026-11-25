package com.ardalorn.arcanvas

import androidx.compose.ui.graphics.Color

/** Colour tokens, carried over from the web version's boilerplate-AA light theme. */
object Tokens {
    val Paper = Color(0xFFEEF2EF)
    val Paper2 = Color(0xFFDEE7E2)
    val Ink = Color(0xFF12221E)
    val Soft = Color(0xFF3A4D47)
    val Edge = Color(0xFF5B736A)
}

data class Swatch(val name: String, val color: Color)

/** First swatch is the theme ink; the rest are the fixed pastels from the web version. */
val Swatches =
    listOf(
        Swatch("Ink", Tokens.Ink),
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

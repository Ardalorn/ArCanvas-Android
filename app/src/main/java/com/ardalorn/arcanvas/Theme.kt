package com.ardalorn.arcanvas

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 theme. Material You (dynamic colour from the wallpaper) is on by default on Android 12+,
 * with no setting to turn it off. Older Android versions get the static palette from [Tokens].
 *
 * It colours the interface only. The canvas paper and the "Ink" brush stay fixed ([Tokens.Paper], [Tokens.Ink]).
 *
 * Light only, as before: the device's dark setting is still ignored.
 */
@Composable
fun ArCanvasTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme =
        if (DYNAMIC_COLOR && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicLightColorScheme(context)
        } else {
            fallbackLightColors()
        }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/**
 * The original boilerplate-AA palette mapped onto Material 3 roles, so the fallback looks as the app did
 * before Material 3. Every text pair is above 7:1 and every boundary above 4:1.
 */
private fun fallbackLightColors(): ColorScheme = lightColorScheme(
    primary = Tokens.Ink,
    onPrimary = Tokens.Paper,
    primaryContainer = Tokens.Paper2,
    onPrimaryContainer = Tokens.Ink,
    secondary = Tokens.Soft,
    onSecondary = Tokens.Paper,
    secondaryContainer = Tokens.Paper2,
    onSecondaryContainer = Tokens.Ink,
    tertiary = Tokens.Soft,
    onTertiary = Tokens.Paper,
    tertiaryContainer = Tokens.Paper2,
    onTertiaryContainer = Tokens.Ink,
    background = Tokens.Paper,
    onBackground = Tokens.Ink,
    surface = Tokens.Paper,
    onSurface = Tokens.Ink,
    surfaceVariant = Tokens.Paper2,
    onSurfaceVariant = Tokens.Soft,
    surfaceContainerLowest = Tokens.Paper,
    surfaceContainerLow = Tokens.Paper,
    surfaceContainer = Tokens.Paper,
    surfaceContainerHigh = Tokens.Paper2,
    surfaceContainerHighest = Tokens.Paper2,
    outline = Tokens.Edge,
    outlineVariant = Tokens.Edge,
)

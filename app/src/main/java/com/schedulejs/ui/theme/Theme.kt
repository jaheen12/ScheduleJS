package com.schedulejs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BlueSteel,
    onPrimary = IvoryMist,
    primaryContainer = Skywash,
    onPrimaryContainer = Ink,
    secondary = Clay,
    onSecondary = IvoryMist,
    secondaryContainer = Sand,
    onSecondaryContainer = Ink,
    tertiary = Olive,
    onTertiary = Ink,
    background = Paper,
    onBackground = Ink,
    surface = IvoryMist,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Slate,
    outline = SlateSoft
)

private val DarkColors = darkColorScheme(
    primary = Sun,
    onPrimary = Night,
    primaryContainer = BlueNight,
    onPrimaryContainer = Paper,
    secondary = Sand,
    onSecondary = Night,
    secondaryContainer = ClayNight,
    onSecondaryContainer = Paper,
    tertiary = Mint,
    onTertiary = Night,
    background = Night,
    onBackground = Paper,
    surface = NightSurface,
    onSurface = Paper,
    surfaceVariant = NightRaised,
    onSurfaceVariant = Fog,
    outline = SlateSoft
)

@Composable
fun ScheduleJsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}

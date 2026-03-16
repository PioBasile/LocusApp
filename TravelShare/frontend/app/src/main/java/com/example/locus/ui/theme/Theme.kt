package com.example.locus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LocusColorScheme = lightColorScheme(
    primary = NavyDark,
    onPrimary = White,
    secondary = GoldPrimary,
    onSecondary = White,
    background = BackgroundWhite,
    onBackground = NavyDark,
    surface = White,
    onSurface = NavyDark,
    surfaceVariant = InputBackground,
    outline = InputBorder,
)

@Composable
fun LocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LocusColorScheme,
        typography = LocusTypography,
        content = content
    )
}
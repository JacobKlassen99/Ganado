package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RanchGreenPrimaryDark,
    onPrimary = RanchGreenOnPrimaryDark,
    primaryContainer = RanchGreenContainerDark,
    onPrimaryContainer = RanchGreenOnContainerDark,
    secondary = RanchEarthSecondaryDark,
    onSecondary = RanchEarthOnSecondaryDark,
    secondaryContainer = RanchEarthContainerDark,
    onSecondaryContainer = RanchEarthOnContainerDark,
    background = RanchBackgroundDark,
    surface = RanchSurfaceDark,
    surfaceVariant = RanchSurfaceVariantDark,
    onSurface = RanchOnSurfaceDark,
    outline = RanchOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = RanchGreenPrimary,
    onPrimary = RanchGreenOnPrimary,
    primaryContainer = RanchGreenContainer,
    onPrimaryContainer = RanchGreenOnContainer,
    secondary = RanchEarthSecondary,
    onSecondary = RanchEarthOnSecondary,
    secondaryContainer = RanchEarthContainer,
    onSecondaryContainer = RanchEarthOnContainer,
    tertiary = RanchGoldTertiary,
    onTertiary = RanchGoldOnTertiary,
    tertiaryContainer = RanchGoldContainer,
    onTertiaryContainer = RanchGoldOnContainer,
    background = RanchBackgroundLight,
    surface = RanchSurfaceLight,
    surfaceVariant = RanchSurfaceVariantLight,
    onSurface = RanchOnSurfaceLight,
    outline = RanchOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted ranch color scheme by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

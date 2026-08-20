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
    primary = BoldPrimaryDark,
    onPrimary = BoldOnPrimaryDark,
    primaryContainer = BoldPrimaryContainerDark,
    onPrimaryContainer = BoldOnPrimaryContainerDark,
    secondary = BoldSecondaryDark,
    onSecondary = BoldOnSecondaryDark,
    secondaryContainer = BoldSecondaryContainerDark,
    onSecondaryContainer = BoldOnSecondaryContainerDark,
    tertiary = BoldTertiaryDark,
    onTertiary = BoldOnTertiaryDark,
    tertiaryContainer = BoldTertiaryContainerDark,
    onTertiaryContainer = BoldOnTertiaryContainerDark,
    background = BoldBackgroundDark,
    surface = BoldSurfaceDark,
    surfaceVariant = BoldSurfaceVariantDark,
    outline = BoldOutlineDark,
    outlineVariant = BoldOutlineVariantDark,
    onBackground = BoldOnSurfaceDark,
    onSurface = BoldOnSurfaceDark,
    onSurfaceVariant = BoldOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = BoldPrimaryLight,
    onPrimary = BoldOnPrimaryLight,
    primaryContainer = BoldPrimaryContainerLight,
    onPrimaryContainer = BoldOnPrimaryContainerLight,
    secondary = BoldSecondaryLight,
    onSecondary = BoldOnSecondaryLight,
    secondaryContainer = BoldSecondaryContainerLight,
    onSecondaryContainer = BoldOnSecondaryContainerLight,
    tertiary = BoldTertiaryLight,
    onTertiary = BoldOnTertiaryLight,
    tertiaryContainer = BoldTertiaryContainerLight,
    onTertiaryContainer = BoldOnTertiaryContainerLight,
    background = BoldBackgroundLight,
    surface = BoldSurfaceLight,
    surfaceVariant = BoldSurfaceVariantLight,
    outline = BoldOutlineLight,
    outlineVariant = BoldOutlineVariantLight,
    onBackground = BoldOnSurfaceLight,
    onSurface = BoldOnSurfaceLight,
    onSurfaceVariant = BoldOnSurfaceVariantLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted BCS theme by default for consistency
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

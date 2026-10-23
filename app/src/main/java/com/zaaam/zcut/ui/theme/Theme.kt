package com.zaaam.zcut.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ZcutDarkColors = darkColorScheme(
    primary = ZcutLime,
    onPrimary = ZcutOnPrimary,
    primaryContainer = ZcutSurface3,
    onPrimaryContainer = ZcutLime,
    secondary = ZcutCyan,
    onSecondary = ZcutOnPrimary,
    tertiary = ZcutCoral,
    onTertiary = ZcutOnPrimary,
    background = ZcutBackground,
    onBackground = ZcutText,
    surface = ZcutSurface,
    onSurface = ZcutText,
    surfaceVariant = ZcutSurface2,
    onSurfaceVariant = ZcutMuted,
    surfaceContainer = ZcutSurface3,
    surfaceContainerHigh = ZcutSurface3,
    surfaceContainerLow = ZcutSurface2,
    outline = ZcutOutline,
    outlineVariant = ZcutOutline,
    error = ZcutRed,
    onError = ZcutOnPrimary
)

private val ZcutLightColors = lightColorScheme(
    primary = ZcutLightPrimary,
    onPrimary = ZcutLightOnPrimary,
    primaryContainer = ZcutLightSurface2,
    onPrimaryContainer = ZcutLightPrimary,
    secondary = ZcutLightSecondary,
    onSecondary = ZcutLightOnPrimary,
    tertiary = ZcutLightTertiary,
    onTertiary = ZcutLightOnPrimary,
    background = ZcutLightBackground,
    onBackground = ZcutLightText,
    surface = ZcutLightSurface,
    onSurface = ZcutLightText,
    surfaceVariant = ZcutLightSurface2,
    onSurfaceVariant = ZcutLightMuted,
    surfaceContainer = ZcutLightSurface2,
    surfaceContainerHigh = ZcutLightSurface2,
    surfaceContainerLow = ZcutLightSurface,
    outline = ZcutLightOutline,
    outlineVariant = ZcutLightOutline,
    error = ZcutRed,
    onError = ZcutLightOnPrimary
)

/**
 * Dark-first theme. The product is a studio tool, so dark is the default even
 * when the device is in light mode; callers can opt into light explicitly.
 */
@Composable
fun ZcutTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ZcutDarkColors else ZcutLightColors,
        typography = ZcutTypography,
        shapes = ZcutShapes,
        content = content
    )
}

/** Exposes whether the system prefers dark, for callers that want to follow it. */
@Composable
fun systemPrefersDark(): Boolean = isSystemInDarkTheme()

package com.streamsphere.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Brand Colors ──────────────────────────────────────────────────────────────

val Primary       = Color(0xFF4F8EF7)   // Electric blue
val OnPrimary     = Color(0xFFFFFFFF)
val Secondary     = Color(0xFFB76EF7)   // Violet
val OnSecondary   = Color(0xFFFFFFFF)
val Tertiary      = Color(0xFF3ECF8E)   // Emerald

val BgDark        = Color(0xFF080C14)
val SurfaceDark   = Color(0xFF111827)
val Surface2Dark  = Color(0xFF1A2235)
val OutlineDark   = Color(0xFF2A3652)
val TextMuted     = Color(0xFF6B7A99)

val NepalRed      = Color(0xFFE53E3E)
val IndiaOrange   = Color(0xFFF6A623)
val ScienceBlue   = Color(0xFF5A9FD4)
val MusicPurple   = Color(0xFFB76EF7)

private val DarkColorScheme = darkColorScheme(
    primary          = Primary,
    onPrimary        = OnPrimary,
    primaryContainer = Color(0xFF1A2E4F),
    secondary        = Secondary,
    onSecondary      = OnSecondary,
    tertiary         = Tertiary,
    background       = BgDark,
    surface          = SurfaceDark,
    surfaceVariant   = Surface2Dark,
    outline          = OutlineDark,
    onBackground     = Color(0xFFE8EDF5),
    onSurface        = Color(0xFFE8EDF5),
    onSurfaceVariant = TextMuted,
    error            = Color(0xFFFC8181)
)

private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF2563EB),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFDEEAFF),
    secondary        = Color(0xFF7C3AED),
    onSecondary      = Color.White,
    tertiary         = Color(0xFF059669),
    background       = Color(0xFFF1F5FB),
    surface          = Color(0xFFFFFFFF),
    surfaceVariant   = Color(0xFFE8EDF5),
    outline          = Color(0xFFD1D9E6),
    onBackground     = Color(0xFF0F172A),
    onSurface        = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun StreamSphereTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}

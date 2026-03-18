package com.willowtree.vocable.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// Force Dark-like scheme because the app design is fundamentally dark blue
private val VocableColorScheme = darkColorScheme(
    primary = ColorPrimary,
    onPrimary = TextColor,
    primaryContainer = ColorPrimary,
    onPrimaryContainer = TextColor,
    
    secondary = ColorAccent,
    onSecondary = ColorPrimaryDark, // Contrast check needed, but typical for accent
    
    tertiary = CategoryColor,
    onTertiary = TextColor,
    
    background = ColorPrimaryDark,
    onBackground = TextColor,
    
    surface = ColorPrimaryDark,
    onSurface = TextColor,
    
    error = ErrorColor,
    onError = TextColor
)

@Composable
fun VocableTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Draw behind system bars and paint our own background instead of using statusBarColor
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // Icons should be light (white) because background is dark blue
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false

            val controller = WindowCompat.getInsetsController(window, view)
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        }
    }
    MaterialTheme(
        colorScheme = VocableColorScheme,
        typography = Typography,
    ) {
        // Ensure a full-screen dark background so content under status bar looks correct in app and previews
        Surface(modifier = Modifier.fillMaxSize(), color = VocableColorScheme.background) {
            content()
        }
    }
}

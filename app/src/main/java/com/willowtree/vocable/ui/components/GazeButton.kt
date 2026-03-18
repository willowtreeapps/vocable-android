package com.willowtree.vocable.ui.components

import android.content.SharedPreferences
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.willowtree.vocable.ui.modifiers.gazeClickable
import com.willowtree.vocable.ui.theme.ColorPrimary
import com.willowtree.vocable.ui.theme.TextColor
import com.willowtree.vocable.core.IVocableSharedPreferences
import com.willowtree.vocable.core.VocableSharedPreferences
import org.koin.compose.koinInject

@Composable
fun GazeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = ColorPrimary,
    textColor: Color = TextColor,
    accessibilityLabel: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val isPreviewMode = LocalInspectionMode.current
    val prefs = if (!isPreviewMode) koinInject<IVocableSharedPreferences>() else null

    // In Preview mode, use default dwell time. Otherwise, observe from SharedPreferences.
    val dwellTime = if (isPreviewMode) {
        1000L
    } else {
        val nonNullPrefs = prefs ?: return  // Should never happen
        var currentDwellTime by remember { mutableLongStateOf(nonNullPrefs.getDwellTime()) }

        DisposableEffect(nonNullPrefs) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == VocableSharedPreferences.KEY_DWELL_TIME) {
                    currentDwellTime = nonNullPrefs.getDwellTime()
                }
            }
            nonNullPrefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose {
                nonNullPrefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
        currentDwellTime
    }
    
    VocableButton(
        onClick = onClick,
        modifier = modifier.gazeClickable(
            onClick = onClick,
            interactionSource = interactionSource,
            enabled = enabled,
            dwellTime = dwellTime,
            accessibilityLabel = accessibilityLabel
        ),
        enabled = enabled,
        interactionSource = interactionSource,
        backgroundColor = backgroundColor,
        textColor = textColor,
        content = content
    )
}

// Preview function for GazeButton
@Preview
@Composable
fun GazeButtonPreview() {
    GazeButton(
        onClick = { /* No-op for preview */ },
        content = {
             Text("Gaze Button")
        }
    )
}

package com.willowtree.vocable.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.theme.ColorPrimaryDark
import com.willowtree.vocable.ui.theme.ErrorColor
import com.willowtree.vocable.ui.theme.VocableTheme

/**
 * Shared confirm/cancel dialog used by every reset flow (per-screen icons, the Reset App Settings
 * screen's checkboxes and nuclear option, and Settings' Privacy Policy/Contact Developers exits).
 * [isDestructive] tints the confirm action [ErrorColor] instead of [ColorPrimaryDark] - matches
 * iOS's GazeableAlertViewController .destructive style, which reuses this exact color value.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    cancelText: String = stringResource(R.string.settings_dialog_cancel),
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val confirmColor = if (isDestructive) ErrorColor else ColorPrimaryDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorPrimaryDark.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(if (isLandscape) 0.6f else 0.85f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = ColorPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = ColorPrimaryDark
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GazeButton(
                        onClick = onDismiss,
                        backgroundColor = Color.Transparent,
                        textColor = ColorPrimaryDark
                    ) {
                        Text(
                            text = cancelText.uppercase(),
                            color = ColorPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    GazeButton(
                        onClick = onConfirm,
                        backgroundColor = Color.Transparent,
                        textColor = confirmColor
                    ) {
                        Text(
                            text = confirmText.uppercase(),
                            color = confirmColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 768, heightDp = 480)
@Composable
private fun ConfirmationDialogPreview() {
    VocableTheme {
        ConfirmationDialog(
            title = "Reset Phrases",
            message = "Are you sure you want to reset all phrases to their defaults? This action cannot be undone.",
            confirmText = "Reset",
            onDismiss = {},
            onConfirm = {},
            isDestructive = true
        )
    }
}

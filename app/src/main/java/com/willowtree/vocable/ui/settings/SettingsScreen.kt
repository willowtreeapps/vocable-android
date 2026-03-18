package com.willowtree.vocable.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.theme.ColorPrimaryDark
import com.willowtree.vocable.ui.theme.TextColor
import com.willowtree.vocable.ui.theme.VocableTheme

@Composable
fun SettingsScreen(
    state: SettingsState,
    versionName: String,
    onClose: () -> Unit,
    onEditCategories: () -> Unit,
    onTimingSensitivity: () -> Unit,
    onSelectionMode: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onContactDevs: () -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val numColumns = integerResource(id = R.integer.settings_options_columns)

    // A flag to pass to background buttons so they don't respond when dialog is open
    val dialogOpen = state.dialogType != ExitDialogType.NONE

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GazeButton(
                    onClick = onClose,
                    modifier = Modifier.size(72.dp),
                    enabled = !dialogOpen
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close_settings)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextColor,
                        fontSize = dimensionResource(id = R.dimen.edit_categories_title_text_size).value.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(end = 88.dp) // Visual balance
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Grid Options Section
            val options = listOf(
                OptionItem(stringResource(R.string.edit_categories_title), onEditCategories),
                OptionItem(stringResource(R.string.timing_sensitivity_title), onTimingSensitivity),
                OptionItem(stringResource(R.string.settings_selection_mode), onSelectionMode)
            )

            val rows = options.chunked(numColumns)

            Column(
                modifier = Modifier
                    .weight(3f) // Take up significant space
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowItems.forEach { item ->
                            SettingsButton(
                                text = item.text,
                                onClick = item.onClick,
                                enabled = !dialogOpen,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                        if (rowItems.size < numColumns) {
                            repeat(numColumns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Links Section
            // if landscape put it in one row else column
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsLinkButton(
                        text = stringResource(R.string.privacy_policy),
                        onClick = onPrivacyPolicy,
                        enabled = !dialogOpen,
                        modifier = Modifier.weight(1f)
                    )

                    SettingsLinkButton(
                        text = stringResource(R.string.contact_developers),
                        onClick = onContactDevs,
                        enabled = !dialogOpen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }else{
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsLinkButton(
                        text = stringResource(R.string.privacy_policy),
                        onClick = onPrivacyPolicy,
                        enabled = !dialogOpen,
                        modifier = Modifier.weight(1f)
                    )

                    SettingsLinkButton(
                        text = stringResource(R.string.contact_developers),
                        onClick = onContactDevs,
                        enabled = !dialogOpen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }


            Spacer(modifier = Modifier.weight(0.1f))

            Text(
                text = versionName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // Compose overlay instead of platform AlertDialog so the GazePointer (drawn in MainActivity) can remain on top
        if (dialogOpen) {
            Box(
                modifier = Modifier
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
                            text = stringResource(R.string.settings_dialog_title), 
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = ColorPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = stringResource(R.string.settings_dialog_message), 
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
                                onClick = onDismissDialog,
                                backgroundColor = Color.Transparent,
                                textColor = ColorPrimaryDark
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_dialog_cancel).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            GazeButton(
                                onClick = onConfirmDialog,
                                backgroundColor = Color.Transparent,
                                textColor = ColorPrimaryDark
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_dialog_continue).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class OptionItem(val text: String, val onClick: () -> Unit)

@Composable
fun SettingsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    GazeButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right_32dp),
                contentDescription = null
            )
        }
    }
}

@Composable
fun SettingsLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    GazeButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_launch_32dp),
                contentDescription = null
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 768, heightDp = 480)
@Composable
fun SettingsScreenPreview() {
    VocableTheme {
        SettingsScreen(
            state = SettingsState(),
            versionName = "Preview",
            onClose = {},
            onEditCategories = {},
            onTimingSensitivity = {},
            onSelectionMode = {},
            onPrivacyPolicy = {},
            onContactDevs = {},
            onDismissDialog = {},
            onConfirmDialog = {}
        )
    }
}

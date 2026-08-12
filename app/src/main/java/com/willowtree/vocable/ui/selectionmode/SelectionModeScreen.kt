package com.willowtree.vocable.ui.selectionmode

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.components.ConfirmationDialog
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.theme.ColorPrimary
import com.willowtree.vocable.ui.theme.ColorPrimaryDark
import com.willowtree.vocable.ui.theme.SelectedColor
import com.willowtree.vocable.ui.theme.TextColor
import com.willowtree.vocable.ui.theme.VocableTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun SelectionModeScreen(
    onBack: () -> Unit,
    viewModel: SelectionModeViewModel = koinViewModel()
) {
    val enabled by viewModel.headTrackingEnabled.asFlow().collectAsStateWithLifecycle(initialValue = false)
    val isResetDialogOpen by viewModel.isResetDialogOpen.collectAsStateWithLifecycle()

    SelectionModeContent(
        enabled = enabled,
        isResetDialogOpen = isResetDialogOpen,
        onBack = onBack,
        onToggleHeadTracking = {
            if (!enabled) {
                viewModel.requestHeadTracking()
            } else {
                viewModel.disableHeadTracking()
            }
        },
        onRequestReset = viewModel::requestReset,
        onDismissResetDialog = viewModel::dismissResetDialog,
        onConfirmReset = viewModel::confirmReset
    )
}

@Composable
fun SelectionModeContent(
    enabled: Boolean,
    isResetDialogOpen: Boolean = false,
    onBack: () -> Unit,
    onToggleHeadTracking: () -> Unit,
    onRequestReset: () -> Unit = {},
    onDismissResetDialog: () -> Unit = {},
    onConfirmReset: () -> Unit = {}
) {
    val buttonHeight = dimensionResource(id = R.dimen.selection_mode_button_height)

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.settings_margin_default))
    ) {
        val (titleRef, backButtonRef, resetButtonRef, trackingButtonRef) = createRefs()
        val backButtonSize = dimensionResource(id = R.dimen.settings_close_button_width)

        Text(
            text = stringResource(id = R.string.selection_mode_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor,
                fontSize = dimensionResource(id = R.dimen.settings_title_text_size).value.sp,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.constrainAs(titleRef) {
                top.linkTo(parent.top)
                start.linkTo(backButtonRef.end, margin = 8.dp)
                end.linkTo(resetButtonRef.start, margin = 8.dp)
                width = Dimension.fillToConstraints
            }
        )

        GazeButton(
            onClick = onBack,
            modifier = Modifier
                .size(backButtonSize)
                .constrainAs(backButtonRef) {
                    top.linkTo(titleRef.top, margin = 8.dp)
                    bottom.linkTo(titleRef.bottom)
                    start.linkTo(parent.start)
                }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back_40dp),
                contentDescription = stringResource(R.string.close_settings),
                tint = Color.Unspecified
            )
        }

        GazeButton(
            onClick = onRequestReset,
            accessibilityLabel = stringResource(R.string.reset_selection_mode_title),
            modifier = Modifier
                .size(backButtonSize)
                .constrainAs(resetButtonRef) {
                    top.linkTo(titleRef.top, margin = 8.dp)
                    bottom.linkTo(titleRef.bottom)
                    end.linkTo(parent.end)
                }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_reset),
                contentDescription = stringResource(R.string.reset_selection_mode_title),
                tint = Color.Unspecified
            )
        }

        GazeButton(
            onClick = onToggleHeadTracking,
            modifier = Modifier
                .height(buttonHeight)
                .fillMaxWidth()
                .constrainAs(trackingButtonRef) {
                    top.linkTo(backButtonRef.bottom, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .testTag("selection_mode_head_tracking_button"),
            backgroundColor = ColorPrimary,
            textColor = TextColor
        ) {
            ConstraintLayout(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                val (textRef, switchRef) = createRefs()

                Text(
                    text = stringResource(id = R.string.settings_head_tracking),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.constrainAs(textRef) {
                        start.linkTo(parent.start)
                        centerVerticallyTo(parent)
                    }
                )

                Switch(
                    checked = enabled,
                    onCheckedChange = null,
                    modifier = Modifier
                        .constrainAs(switchRef) {
                            end.linkTo(parent.end)
                            centerVerticallyTo(parent)
                        }
                        .testTag("selection_mode_head_tracking_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SelectedColor,
                        checkedTrackColor = ColorPrimaryDark,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = ColorPrimaryDark
                    )
                )
            }
        }
    }

    if (isResetDialogOpen) {
        ConfirmationDialog(
            title = stringResource(R.string.reset_selection_mode_title),
            message = stringResource(R.string.reset_selection_mode_dialog_message),
            confirmText = stringResource(R.string.settings_reset_dialog_confirm),
            onDismiss = onDismissResetDialog,
            onConfirm = onConfirmReset,
            isDestructive = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SelectionModeScreenPreview() {
    VocableTheme {
        SelectionModeContent(
            enabled = true,
            onBack = {},
            onToggleHeadTracking = {}
        )
    }
}

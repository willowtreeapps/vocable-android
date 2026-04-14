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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.settings.SettingsButton
import com.willowtree.vocable.ui.theme.ColorPrimary
import com.willowtree.vocable.ui.theme.ColorPrimaryDark
import com.willowtree.vocable.ui.theme.SelectedColor
import com.willowtree.vocable.ui.theme.TextColor
import com.willowtree.vocable.ui.theme.VocableTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun SelectionModeScreen(
    onBack: () -> Unit,
    onVoiceSelection: () -> Unit,
    onLanguageSelection: () -> Unit,
    viewModel: SelectionModeViewModel = koinViewModel()
) {
    val enabled by viewModel.headTrackingEnabled.asFlow().collectAsStateWithLifecycle(initialValue = false)
    val selectedVoiceLabel by viewModel.selectedVoiceLabel.collectAsStateWithLifecycle()
    val selectedLanguageLabel by viewModel.selectedLanguageLabel.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLabels()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SelectionModeContent(
        enabled = enabled,
        selectedVoiceLabel = selectedVoiceLabel,
        selectedLanguageLabel = selectedLanguageLabel,
        onBack = onBack,
        onVoiceSelection = onVoiceSelection,
        onLanguageSelection = onLanguageSelection,
        onToggleHeadTracking = {
            if (!enabled) {
                viewModel.requestHeadTracking()
            } else {
                viewModel.disableHeadTracking()
            }
        }
    )
}

@Composable
fun SelectionModeContent(
    enabled: Boolean,
    selectedVoiceLabel: String,
    selectedLanguageLabel: String,
    onBack: () -> Unit,
    onVoiceSelection: () -> Unit,
    onLanguageSelection: () -> Unit,
    onToggleHeadTracking: () -> Unit
) {
    val buttonHeight = dimensionResource(id = R.dimen.selection_mode_button_height)

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.settings_margin_default))
    ) {
        val (titleRef, backButtonRef, trackingButtonRef, voiceButtonRef, languageButtonRef) = createRefs()
        val backButtonSize = dimensionResource(id = R.dimen.settings_close_button_width)

        Text(
            text = stringResource(id = R.string.selection_mode_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor,
                fontSize = dimensionResource(id = R.dimen.settings_title_text_size).value.sp
            ),
            modifier = Modifier.constrainAs(titleRef) {
                top.linkTo(parent.top)
                start.linkTo(backButtonRef.end, margin = 72.dp)
                end.linkTo(parent.end, margin = backButtonSize + 16.dp)
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

        SettingsButton(
            text = stringResource(R.string.settings_voice, selectedVoiceLabel),
            onClick = onVoiceSelection,
            modifier = Modifier
                .height(buttonHeight)
                .fillMaxWidth()
                .constrainAs(voiceButtonRef) {
                    top.linkTo(trackingButtonRef.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        SettingsButton(
            text = stringResource(R.string.settings_language, selectedLanguageLabel),
            onClick = onLanguageSelection,
            modifier = Modifier
                .height(buttonHeight)
                .fillMaxWidth()
                .constrainAs(languageButtonRef) {
                    top.linkTo(voiceButtonRef.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SelectionModeScreenPreview() {
    VocableTheme {
        SelectionModeContent(
            enabled = true,
            selectedVoiceLabel = "Default",
            selectedLanguageLabel = "System Default",
            onBack = {},
            onVoiceSelection = {},
            onLanguageSelection = {},
            onToggleHeadTracking = {}
        )
    }
}

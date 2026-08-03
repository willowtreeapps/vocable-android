package com.willowtree.vocable.ui.settingsvoice

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.settings.SettingsButton
import com.willowtree.vocable.ui.theme.TextColor
import com.willowtree.vocable.ui.theme.VocableTheme

@Composable
fun SettingsVoiceScreen(
    state: SettingsVoiceState,
    onBack: () -> Unit,
    onChangeVoice: () -> Unit,
    onRefreshActiveVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshActiveVoice()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.settings_margin_default))
    ) {
        val (titleRef, backButtonRef, previewRowRef, changeVoiceRowRef) = createRefs()
        val backButtonSize = dimensionResource(id = R.dimen.settings_close_button_width)

        Text(
            text = stringResource(id = R.string.voice_settings_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor,
                fontSize = dimensionResource(id = R.dimen.settings_title_text_size).value.sp
            ),
            modifier = Modifier.constrainAs(titleRef) {
                start.linkTo(backButtonRef.end, margin = backButtonSize + 16.dp)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(previewRowRef) {
                    top.linkTo(backButtonRef.bottom, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_play_circle_40dp),
                contentDescription = stringResource(R.string.voice_settings_preview_content_description),
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = state.activeVoiceDisplayName,
                style = MaterialTheme.typography.titleMedium,
                color = TextColor
            )
        }

        SettingsButton(
            text = stringResource(R.string.voice_settings_change_voice),
            onClick = onChangeVoice,
            modifier = Modifier
                .height(dimensionResource(id = R.dimen.selection_mode_button_height))
                .fillMaxWidth()
                .constrainAs(changeVoiceRowRef) {
                    top.linkTo(previewRowRef.bottom, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, widthDp = 768, heightDp = 480)
@Composable
fun SettingsVoiceScreenPreview() {
    VocableTheme {
        SettingsVoiceScreen(
            state = SettingsVoiceState(activeVoiceDisplayName = "English (United States) – Enhanced"),
            onBack = {},
            onChangeVoice = {},
            onRefreshActiveVoice = {}
        )
    }
}
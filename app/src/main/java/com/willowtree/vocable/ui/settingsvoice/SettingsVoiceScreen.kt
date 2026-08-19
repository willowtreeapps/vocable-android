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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
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
    onPreviewActiveVoice: () -> Unit,
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
        val (titleRef, backButtonRef, previewRowRef, changeVoiceRowRef, footerRef) = createRefs()
        val backButtonSize = dimensionResource(id = R.dimen.settings_close_button_width)

        Text(
            text = stringResource(id = R.string.voice_settings_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor,
                fontSize = dimensionResource(id = R.dimen.settings_title_text_size).value.sp,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.constrainAs(titleRef) {
                start.linkTo(backButtonRef.end, margin = 8.dp)
                end.linkTo(parent.end, margin = 8.dp)
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

        // Reads the active voice's own display name aloud in that voice — not a sample phrase, so
        // it sidesteps #613's still-open sample-phrase decision. Toggles to the stop icon while
        // speaking and back to play once done.
        GazeButton(
            onClick = onPreviewActiveVoice,
            modifier = Modifier
                .height(dimensionResource(id = R.dimen.selection_mode_button_height))
                .fillMaxWidth()
                .constrainAs(previewRowRef) {
                    top.linkTo(backButtonRef.bottom, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(
                        id = if (state.isPreviewPlaying) R.drawable.ic_stop_circle_40dp else R.drawable.ic_play_circle_40dp
                    ),
                    contentDescription = stringResource(
                        if (state.isPreviewPlaying) R.string.voice_settings_stop_preview_content_description
                        else R.string.voice_settings_preview_content_description
                    ),
                    tint = Color.Unspecified
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = state.activeVoiceDisplayName,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        SettingsButton(
            text = stringResource(R.string.voice_settings_change_voice),
            onClick = onChangeVoice,
            modifier = Modifier
                .height(dimensionResource(id = R.dimen.selection_mode_button_height))
                .fillMaxWidth()
                .constrainAs(changeVoiceRowRef) {
                    top.linkTo(previewRowRef.bottom, margin = 12.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        Text(
            text = stringResource(R.string.voice_settings_footer),
            style = MaterialTheme.typography.bodyMedium,
            color = TextColor,
            modifier = Modifier.constrainAs(footerRef) {
                top.linkTo(changeVoiceRowRef.bottom, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
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
            state = SettingsVoiceState(activeVoiceDisplayName = "English (United States) Voice 1"),
            onBack = {},
            onChangeVoice = {},
            onRefreshActiveVoice = {},
            onPreviewActiveVoice = {}
        )
    }
}
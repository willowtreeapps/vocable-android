package com.willowtree.vocable.ui.voiceselection

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.willowtree.vocable.R
import com.willowtree.vocable.core.VocableTextToSpeech
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.modifiers.horizontalPageSwipe
import com.willowtree.vocable.ui.theme.VocableTheme
import kotlin.math.ceil

private const val ITEMS_PER_PAGE = 5

@Composable
fun VoiceSelectionScreen(
    state: VoiceSelectionState,
    onBack: () -> Unit,
    onVoiceSelected: (String?) -> Unit,
    onDownloadVoice: () -> Unit,
    onRefreshVoices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshVoices()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var pageIndex by remember { mutableIntStateOf(0) }
    val totalPages = remember(state.voices) {
        maxOf(1, ceil(state.voices.size.toFloat() / ITEMS_PER_PAGE).toInt())
    }
    val currentPageItems = remember(state.voices, pageIndex) {
        state.voices.chunked(ITEMS_PER_PAGE).getOrElse(pageIndex) { emptyList() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .horizontalPageSwipe(
                onSwipeLeft = { pageIndex = if (pageIndex > 0) pageIndex - 1 else totalPages - 1 },
                onSwipeRight = { pageIndex = (pageIndex + 1) % totalPages }
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GazeButton(
                onClick = onBack,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close_voice_selection)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.voice_selection_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        GazeButton(
            onClick = { onVoiceSelected(null) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.voice_default),
                modifier = Modifier.padding(16.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(ITEMS_PER_PAGE) { i ->
                val voice = currentPageItems.getOrNull(i)
                if (voice != null) {
                    VoiceOptionRow(
                        voice = voice,
                        isSelected = state.selectedVoiceName == voice.name,
                        onClick = {
                            if (voice.isDownloaded) {
                                onVoiceSelected(voice.name)
                            } else {
                                onDownloadVoice()
                            }
                        },
                        modifier = if (isLandscape) Modifier.fillMaxWidth()
                                   else Modifier.weight(1f).fillMaxWidth()
                    )
                } else if (!isLandscape) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pagingButtonSize = dimensionResource(id = R.dimen.phrases_paging_button_height)

            GazeButton(
                onClick = { pageIndex = if (pageIndex > 0) pageIndex - 1 else totalPages - 1 },
                modifier = Modifier.size(pagingButtonSize)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phrases_arrow_back_blue),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
            }

            Text(
                text = stringResource(R.string.phrases_page_number, pageIndex + 1, totalPages),
                style = MaterialTheme.typography.bodyLarge
            )

            GazeButton(
                onClick = { pageIndex = (pageIndex + 1) % totalPages },
                modifier = Modifier.size(pagingButtonSize)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phrases_arrow_forward_blue),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
            }
        }
    }
}

@Composable
private fun VoiceOptionRow(
    voice: VocableTextToSpeech.VoiceOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GazeButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = voice.displayName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            when {
                !voice.isDownloaded -> Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_down_40dp),
                    contentDescription = stringResource(R.string.voice_download)
                )
                isSelected -> Text(
                    text = stringResource(R.string.selected),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview
@Composable
private fun VoiceSelectionScreenPreview() {
    VocableTheme {
        VoiceSelectionScreen(
            state = VoiceSelectionState(
                voices = listOf(
                    VocableTextToSpeech.VoiceOption("voice_1", "English (United States) – Enhanced", java.util.Locale.US, isDownloaded = true),
                    VocableTextToSpeech.VoiceOption("voice_2", "English (United States) – Standard", java.util.Locale.US, isDownloaded = false)
                ),
                selectedVoiceName = "voice_1"
            ),
            onBack = {},
            onVoiceSelected = {},
            onDownloadVoice = {},
            onRefreshVoices = {}
        )
    }
}

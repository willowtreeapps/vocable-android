package com.willowtree.vocable.ui.voiceselection

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.willowtree.vocable.R
import com.willowtree.vocable.core.VocableTextToSpeech
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.components.VocablePagination
import com.willowtree.vocable.ui.modifiers.horizontalPageSwipe
import com.willowtree.vocable.ui.theme.VocableTheme
import kotlin.math.ceil

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
    val itemsPerPage = if (isLandscape) 3 else 5
    val padding = if (isLandscape) 16.dp else 24.dp
    val closeButtonSize = if (isLandscape) 48.dp else 72.dp

    var pageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isLandscape) { pageIndex = 0 }

    val totalPages = remember(state.voices, itemsPerPage) {
        maxOf(1, ceil(state.voices.size.toFloat() / itemsPerPage).toInt())
    }
    val currentPageItems = remember(state.voices, pageIndex, itemsPerPage) {
        state.voices.chunked(itemsPerPage).getOrElse(pageIndex) { emptyList() }
    }

    // Paging wraps in both directions, matching iOS's carousel. Shared by the swipe gestures and
    // the pagination buttons so the two can't drift apart.
    val goToPreviousPage = { pageIndex = if (pageIndex > 0) pageIndex - 1 else totalPages - 1 }
    val goToNextPage = { pageIndex = (pageIndex + 1) % totalPages }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .horizontalPageSwipe(
                onSwipeLeft = goToPreviousPage,
                onSwipeRight = goToNextPage
            ),
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GazeButton(
                onClick = onBack,
                modifier = Modifier.size(closeButtonSize)
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

        if (state.voices.isEmpty()) {
            VoiceSelectionEmptyState(modifier = Modifier.weight(1f))
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 12.dp)
            ) {
                repeat(itemsPerPage) { i ->
                    val voice = currentPageItems.getOrNull(i)
                    if (voice != null) {
                        VoiceOptionRow(
                            voice = voice,
                            isSelected = state.selectedVoiceName == voice.name,
                            isLandscape = isLandscape,
                            onClick = {
                                if (voice.isDownloaded) {
                                    onVoiceSelected(voice.name)
                                } else {
                                    onDownloadVoice()
                                }
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        VocablePagination(
            pageIndex = pageIndex,
            pageCount = totalPages,
            onPreviousPage = goToPreviousPage,
            onNextPage = goToNextPage,
            buttonSize = if (isLandscape) 40.dp
            else dimensionResource(id = R.dimen.phrases_paging_button_height),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Shown when no installed voice matches the current language/region, mirroring iOS's
 * `VoicePickerEmptyStateConfiguration`. iOS leaves its pagination visible (but disabled) behind
 * this state, so this doesn't hide it either.
 */
@Composable
private fun VoiceSelectionEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.voice_empty_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.voice_empty_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VoiceOptionRow(
    voice: VocableTextToSpeech.VoiceOption,
    isSelected: Boolean,
    isLandscape: Boolean,
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
                .padding(if (isLandscape) 8.dp else 16.dp),
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
                isSelected -> Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = stringResource(R.string.selected)
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

@Preview
@Composable
private fun VoiceSelectionScreenEmptyPreview() {
    VocableTheme {
        VoiceSelectionScreen(
            state = VoiceSelectionState(voices = emptyList(), selectedVoiceName = null),
            onBack = {},
            onVoiceSelected = {},
            onDownloadVoice = {},
            onRefreshVoices = {}
        )
    }
}

package com.willowtree.vocable.ui.voiceselection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
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
import com.willowtree.vocable.ui.theme.ColorPrimary
import com.willowtree.vocable.ui.theme.VocableTheme
import kotlin.math.ceil

@Composable
fun VoiceSelectionScreen(
    state: VoiceSelectionState,
    onBack: () -> Unit,
    onVoiceSelected: (String?) -> Unit,
    onRefreshVoices: () -> Unit,
    onPreviewVoice: (VocableTextToSpeech.VoiceOption, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val previewSampleFormat = stringResource(R.string.voice_preview_sample)
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

    // Grid shape comes from per-breakpoint resources, mirroring iOS's size-class switch in
    // VoicePickerViewController.updateLayoutForCurrentTraitCollection(): one column for phone
    // portrait (hCompact_vRegular), two everywhere else. Row counts are tuned per breakpoint so a
    // full page fills the available height — see Documentation/work-log/644-*.md.
    val voiceColumns = integerResource(id = R.integer.voice_columns)
    val voiceRows = integerResource(id = R.integer.voice_rows)
    val itemsPerPage = voiceColumns * voiceRows

    val screenMargin = dimensionResource(id = R.dimen.voice_screen_margin)
    val sectionSpacing = dimensionResource(id = R.dimen.voice_section_spacing)
    val rowSpacing = dimensionResource(id = R.dimen.voice_row_spacing)
    val columnSpacing = dimensionResource(id = R.dimen.voice_column_spacing)

    var pageIndex by remember { mutableIntStateOf(0) }

    // Page capacity changes on rotation and on any resize that crosses a breakpoint (multi-window,
    // foldables), so reset against the capacity itself rather than orientation alone.
    LaunchedEffect(itemsPerPage) { pageIndex = 0 }

    val totalPages = remember(state.voices, itemsPerPage) {
        maxOf(1, ceil(state.voices.size.toFloat() / itemsPerPage).toInt())
    }
    // The list can shrink underneath us: onRefreshVoices() re-reads the installed voices on every
    // ON_RESUME, so uninstalling a voice in system Settings and returning to Vocable drops the
    // count — and #618 now hides undownloaded voices outright rather than leaving a greyed row
    // behind. Without this clamp, an out-of-range pageIndex renders a blank page.
    LaunchedEffect(totalPages) {
        if (pageIndex >= totalPages) pageIndex = totalPages - 1
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
            .padding(screenMargin)
            .horizontalPageSwipe(
                onSwipeLeft = goToPreviousPage,
                onSwipeRight = goToNextPage
            ),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GazeButton(
                onClick = onBack,
                modifier = Modifier.size(dimensionResource(id = R.dimen.voice_close_button_size))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close_voice_selection)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.voice_settings_change_voice),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        if (state.voices.isEmpty()) {
            VoiceSelectionEmptyState(modifier = Modifier.weight(1f))
        } else {
            // Fixed row×column slots, as in PresetsScreen: a tile's position on the page must not
            // depend on how many voices happen to be installed. A short last page leaves its
            // trailing slots empty rather than reflowing, so a gaze target never shifts under the
            // user mid-dwell.
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(rowSpacing)
            ) {
                for (rowIndex in 0 until voiceRows) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(columnSpacing)
                    ) {
                        for (colIndex in 0 until voiceColumns) {
                            val voice = currentPageItems.getOrNull(rowIndex * voiceColumns + colIndex)

                            if (voice != null) {
                                VoiceOptionRow(
                                    voice = voice,
                                    isSelected = state.selectedVoiceName == voice.name,
                                    isPlaying = state.previewingVoiceName == voice.name,
                                    onClick = { onVoiceSelected(voice.name) },
                                    onPreviewClick = { onPreviewVoice(voice, previewSampleFormat) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        VocablePagination(
            pageIndex = pageIndex,
            pageCount = totalPages,
            onPreviousPage = goToPreviousPage,
            onNextPage = goToNextPage,
            buttonSize = dimensionResource(id = R.dimen.voice_paging_button_size),
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

/**
 * Tapping the play chip speaks a fixed sample phrase (`voice_preview_sample`, with [voice]'s display
 * name substituted in) in that voice. It toggles to the stop icon while speaking (driven by
 * [isPlaying], sourced from the global `VocableTextToSpeech.isSpeakingFlow`) and back to play once done.
 *
 * The row fills the height of its grid slot, so the name tile grows on roomier breakpoints. The
 * square play chip is capped at `voice_play_chip_max_size` instead — left unbounded it would track
 * the row height and balloon into an oversized square on a tablet's taller rows.
 */
@Composable
private fun VoiceOptionRow(
    voice: VocableTextToSpeech.VoiceOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onPreviewClick: () -> Unit = {}
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(id = R.dimen.voice_row_content_spacing)
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GazeButton(
            onClick = onPreviewClick,
            backgroundColor = ColorPrimary,
            modifier = Modifier
                // heightIn sits outside fillMaxHeight so it caps the incoming constraint before
                // the fill resolves; aspectRatio then squares off the capped height.
                .heightIn(max = dimensionResource(id = R.dimen.voice_play_chip_max_size))
                .fillMaxHeight()
                .aspectRatio(1f)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPlaying) R.drawable.ic_stop_circle_40dp else R.drawable.ic_play_circle_40dp
                ),
                contentDescription = stringResource(
                    if (isPlaying) R.string.voice_settings_stop_preview_content_description
                    else R.string.voice_settings_preview_content_description
                ),
                tint = Color.Unspecified
            )
        }

        GazeButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.voice_row_text_padding)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = voice.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_40dp),
                        contentDescription = stringResource(R.string.selected)
                    )
                }
            }
        }
    }
}

private val previewVoices = List(7) { index ->
    VocableTextToSpeech.VoiceOption(
        "voice_${index + 1}",
        "English (United States) Voice ${index + 1}",
        java.util.Locale.US
    )
}

/**
 * Seven voices, so every breakpoint's last page is a partial one — that's the case where the empty
 * trailing slots have to hold their positions instead of the remaining tiles reflowing.
 */
@Preview(name = "Phone portrait", device = "spec:width=393dp,height=851dp,dpi=440")
@Preview(
    name = "Phone landscape",
    device = "spec:width=393dp,height=851dp,dpi=440,orientation=landscape"
)
@Preview(name = "Tablet portrait", device = "spec:width=800dp,height=1280dp,dpi=240")
@Preview(
    name = "Tablet landscape",
    device = "spec:width=800dp,height=1280dp,dpi=240,orientation=landscape"
)
@Composable
private fun VoiceSelectionScreenPreview() {
    VocableTheme {
        VoiceSelectionScreen(
            state = VoiceSelectionState(
                voices = previewVoices,
                selectedVoiceName = "voice_1"
            ),
            onBack = {},
            onVoiceSelected = {},
            onRefreshVoices = {},
            onPreviewVoice = { _, _ -> }
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
            onRefreshVoices = {},
            onPreviewVoice = { _, _ -> }
        )
    }
}

/**
 * Design-review mock only, matching the iOS Change Voice screenshots — the real screen can't show
 * human voice names (Android's `TextToSpeech`/`Voice` API has no friendly-name field, unlike
 * `AVSpeechSynthesisVoice.name` on iOS). so this preview stands in with placeholder names to review
 * the per-row play chip / checkmark layout. Not backed by any real state or data source.
 */
@Preview(showBackground = true)
@Composable
private fun VoiceOptionRowMockedNamesPreview() {
    data class MockVoiceRow(val name: String, val isSelected: Boolean = false, val isPlaying: Boolean = false)

    val mockRows = listOf(
        MockVoiceRow("Daniel"),
        MockVoiceRow("Fred"),
        MockVoiceRow("Junior"),
        MockVoiceRow("Karen", isSelected = true),
        MockVoiceRow("Kathy", isPlaying = true),
        MockVoiceRow("Moira"),
        MockVoiceRow("Ralph"),
        MockVoiceRow("Rishi")
    )

    VocableTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            mockRows.forEach { mock ->
                VoiceOptionRow(
                    voice = VocableTextToSpeech.VoiceOption(mock.name, mock.name, java.util.Locale.US),
                    isSelected = mock.isSelected,
                    isPlaying = mock.isPlaying,
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }
        }
    }
}

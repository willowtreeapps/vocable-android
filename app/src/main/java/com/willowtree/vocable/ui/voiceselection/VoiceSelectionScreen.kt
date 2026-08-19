package com.willowtree.vocable.ui.voiceselection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * Slot reserved for the trailing checkmark on every row, so a name is auto-sized against the same
 * width whether or not its voice is selected. Deliberately smaller than `ic_check_40dp`'s 40dp
 * intrinsic size — the glyph inside that vector is 24dp, and every dp reserved here is a dp taken
 * off the name's width, which is already the tight dimension in a two-column tile.
 */
private val CHECKMARK_SIZE = 24.dp

/**
 * How many voice rows a page of [gridHeight] holds, given [rowSpacing] between them: the largest n
 * whose n rows plus (n − 1) gaps still leave every row at least [rowMinHeight] tall.
 *
 * This is the picker's page capacity, so it also decides how many pages the voice list needs. It is
 * derived rather than read from a resource because `sw###dp` qualifiers constrain width and never
 * height — see the call site for why a fixed count cannot be right for every device in a bucket.
 *
 * Floored at 1: a viewport too short for even one full row still renders one, rather than an empty
 * page with no way to pick a voice.
 */
internal fun voiceRowCount(gridHeight: Dp, rowMinHeight: Dp, rowSpacing: Dp): Int =
    maxOf(1, ((gridHeight + rowSpacing) / (rowMinHeight + rowSpacing)).toInt())

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

    // Column count comes from a per-breakpoint resource, mirroring iOS's size-class switch in
    // VoicePickerViewController.updateLayoutForCurrentTraitCollection(): one column for phone
    // portrait (hCompact_vRegular), two everywhere else.
    val voiceColumns = integerResource(id = R.integer.voice_columns)

    val screenMargin = dimensionResource(id = R.dimen.voice_screen_margin)
    val sectionSpacing = dimensionResource(id = R.dimen.voice_section_spacing)
    val rowSpacing = dimensionResource(id = R.dimen.voice_row_spacing)
    val columnSpacing = dimensionResource(id = R.dimen.voice_column_spacing)
    val rowMinHeight = dimensionResource(id = R.dimen.voice_row_min_height)
    val closeButtonSize = dimensionResource(id = R.dimen.voice_close_button_size)
    val pagingButtonSize = dimensionResource(id = R.dimen.voice_paging_button_size)

    var pageIndex by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // The row count is derived from the page's own height instead of read from a `voice_rows`
        // resource, because `sw###dp` qualifiers constrain width and never height: one count cannot
        // be right for every device in a bucket. #644 shipped fixed counts, and the empty band
        // below a *full* page was the result — 5 rows of 60dp in 635dp of phone-portrait grid, and
        // 7 of 80dp in 1024dp on tablet portrait. A count tuned upward for those would in turn
        // overflow a 640dp-tall phone, which lands in the same `values` dir.
        //
        // `maxHeight` is the whole screen, so the chrome above and below the grid comes off first.
        // The header row is as tall as its close button and the pager as tall as its buttons at
        // default font scale; at larger scales their text can outgrow them, so this is an estimate.
        // That is safe: the count only has to be close, because the rows themselves stretch to fill
        // whatever the grid actually measures. Being off by one costs a few dp of row height.
        val gridHeight = maxHeight - (screenMargin * 2) - closeButtonSize -
            (sectionSpacing * 2) - pagingButtonSize
        val voiceRows = voiceRowCount(gridHeight, rowMinHeight, rowSpacing)
        val itemsPerPage = voiceColumns * voiceRows

        // Page capacity changes on rotation and on any resize that crosses a breakpoint
        // (multi-window, foldables), so reset against the capacity itself rather than orientation.
        LaunchedEffect(itemsPerPage) { pageIndex = 0 }

        val totalPages = remember(state.voices, itemsPerPage) {
            maxOf(1, ceil(state.voices.size.toFloat() / itemsPerPage).toInt())
        }
        // The list can shrink underneath us: onRefreshVoices() re-reads the installed voices on
        // every ON_RESUME, so uninstalling a voice in system Settings and returning to Vocable
        // drops the count — and #618 now hides undownloaded voices outright rather than leaving a
        // greyed row behind. Without this clamp, an out-of-range pageIndex renders a blank page.
        LaunchedEffect(totalPages) {
            if (pageIndex >= totalPages) pageIndex = totalPages - 1
        }

        val currentPageItems = remember(state.voices, pageIndex, itemsPerPage) {
            state.voices.chunked(itemsPerPage).getOrElse(pageIndex) { emptyList() }
        }

        // Paging wraps in both directions, matching iOS's carousel. Shared by the swipe gestures
        // and the pagination buttons so the two can't drift apart.
        val goToPreviousPage = { pageIndex = if (pageIndex > 0) pageIndex - 1 else totalPages - 1 }
        val goToNextPage = { pageIndex = (pageIndex + 1) % totalPages }

        Column(
            modifier = Modifier
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
                    modifier = Modifier.size(closeButtonSize)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close_voice_selection),
                        modifier = Modifier.size(
                            dimensionResource(id = R.dimen.voice_close_icon_size)
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.voice_settings_change_voice),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = dimensionResource(id = R.dimen.settings_title_text_size).value.sp
                    )
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            if (state.voices.isEmpty()) {
                VoiceSelectionEmptyState(modifier = Modifier.weight(1f))
            } else {
                // Fixed row×column slots, as in PresetsScreen: a tile's size and its column are
                // the same on every page, and a row that runs out of voices leaves its trailing
                // columns empty rather than reflowing the remaining tiles across the row.
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    // Rows take the exact height a *full* page would give them, measured against
                    // the grid's real height rather than the estimate used for the count above. A
                    // full page therefore fills the page exactly — no band of dead space at the
                    // bottom, and no row pushed off a shorter screen. Because `voiceRows` came from
                    // `voice_row_min_height`, that height stays close to the chip-matched height
                    // design asked for: at least it, and at most one row-pitch more.
                    val rowHeight = (maxHeight - rowSpacing * (voiceRows - 1)) / voiceRows

                    // A short page renders only its occupied rows and insets them from the top, so
                    // the leftover height is shared with the space under the header instead of all
                    // landing between the last tile and the pager. Tile *size* never changes, and a
                    // full page is unaffected — its leftover is zero, so the inset is too.
                    //
                    // The inset is half the leftover (i.e. the group is centred) but **capped at
                    // half a row pitch**. Centring alone reads as intended when a page is nearly
                    // full — one missing row on phone portrait puts a balanced 40dp above and below
                    // — but a sparse page floats: 9 installed voices fill 5 of tablet portrait's 10
                    // rows, which uncapped would sit 276dp down the page. The cap keeps that case
                    // near the top at 52dp.
                    //
                    // TRADE-OFF, deliberate: this is the one place where a tile's vertical position
                    // depends on how many voices are on the page, so paging onto a short last page
                    // shifts the rows down by up to half a row. Fixed positions are otherwise an
                    // accessibility contract for gaze users (CLAUDE.md), and iOS keeps this screen
                    // top-anchored — `CarouselGridLayout.alignment` defaults to `.top` and
                    // VoicePickerViewController never overrides it, unlike
                    // ListeningResponseContentViewController which does set `.center`. Balancing the
                    // gap was asked for on #667; the cap is what keeps the resulting shift bounded
                    // to half a row rather than half a page.
                    val occupiedRows = ceil(
                        currentPageItems.size.toFloat() / voiceColumns
                    ).toInt()
                    val leftover = maxHeight -
                        (rowHeight * occupiedRows + rowSpacing * (occupiedRows - 1))
                    val topInset = minOf(leftover / 2, (rowHeight + rowSpacing) / 2)
                        .coerceAtLeast(0.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topInset),
                        verticalArrangement = Arrangement.spacedBy(rowSpacing)
                    ) {
                        for (rowIndex in 0 until occupiedRows) {
                            Row(
                                modifier = Modifier
                                    .height(rowHeight)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(columnSpacing)
                            ) {
                                for (colIndex in 0 until voiceColumns) {
                                    val voice =
                                        currentPageItems.getOrNull(
                                            rowIndex * voiceColumns + colIndex
                                        )

                                    if (voice != null) {
                                        VoiceOptionRow(
                                            voice = voice,
                                            isSelected = state.selectedVoiceName == voice.name,
                                            isPlaying = state.previewingVoiceName == voice.name,
                                            onClick = { onVoiceSelected(voice.name) },
                                            onPreviewClick = {
                                                onPreviewVoice(voice, previewSampleFormat)
                                            },
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
            }

            VocablePagination(
                pageIndex = pageIndex,
                pageCount = totalPages,
                onPreviousPage = goToPreviousPage,
                onNextPage = goToNextPage,
                buttonSize = pagingButtonSize,
                iconSize = dimensionResource(id = R.dimen.voice_paging_icon_size),
                modifier = Modifier.fillMaxWidth()
            )
        }
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
            .padding(horizontal = dimensionResource(id = R.dimen.voice_empty_state_padding)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.voice_empty_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(dimensionResource(id = R.dimen.voice_empty_state_spacing))
        )

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
 * The row's height is set by its parent — `voice_row_min_height` or a little more, see the grid in
 * [VoiceSelectionScreen] — and the play chip fills that height as a square, so chip and name tile
 * always match, the shape the design calls for.
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
                // The row is a fixed height, so filling it and squaring off gives a chip that is
                // exactly as tall as the name tile beside it.
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
                tint = Color.Unspecified,
                modifier = Modifier.size(dimensionResource(id = R.dimen.voice_play_icon_size))
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
                // Names run to ~31 characters ("English (United States) Voice 9") and a two-column
                // tile only affords ~213dp of text width, so the text is auto-sized to fit a single
                // line — the same BasicText/TextAutoSize treatment PresetsScreen gives its phrase
                // tiles. Kept to one line deliberately: wrapping put the trailing "Voice N" index
                // alone on line two, and that index is the only thing distinguishing one row from
                // the next.
                //
                // Overflow must stay `Ellipsis`, NOT `MiddleEllipsis`: with a middle-ellipsis
                // overflow, auto-size treats the text as always fitting (it can truncate to any
                // width) and so never steps down, which truncated the name even at default font
                // scale. Verified on device both ways.
                //
                // Line budget is font-scale dependent. The auto-size floor is in sp, so it grows
                // with the user's font-size setting; past ~1.25x the name cannot fit one line at
                // the tightest breakpoint however far the step goes, so a second line is allowed
                // from there. A flat `maxLines = 2` is not an option: at default scale auto-size
                // would then keep 16sp and wrap, leaving the index orphaned on line two rather
                // than shrinking to fit one line.
                //
                // KNOWN LIMITATION: that second line usually cannot be used, because a row is only
                // about `voice_row_min_height` tall. At 2x, one line does not fit widthwise (~340dp
                // needed vs ~213dp) and two do not fit heightwise (2 x 48dp line height in an 80dp
                // row), so the name still truncates and loses its trailing index. Not solvable in
                // layout while rows stay chip-height; see Documentation/work-log/644-*.md for the
                // options.
                BasicText(
                    text = voice.displayName,
                    // Must be a filling weight, i.e. a definite width. With `fill = false` the text
                    // is measured at its desired width instead, auto-size stops shrinking to fit,
                    // and the name truncates ("English (Unite…ates) Voice 1") — verified on device.
                    // The cost is that the checkmark sits at the tile's trailing edge rather than
                    // immediately after the text; showing the whole name matters more.
                    modifier = Modifier.weight(1f),
                    // BasicText, unlike Text, does not read LocalContentColor — so the color is
                    // pulled in explicitly to keep VocableButton's dwell-press color flip working.
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = LocalContentColor.current
                    ),
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 12.sp,
                        maxFontSize = 16.sp,
                        stepSize = 0.5.sp
                    ),
                    maxLines = if (LocalDensity.current.fontScale > 1.25f) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )

                // The checkmark's slot is reserved on every row, not just the selected one, so a
                // name wraps at the same point whether or not its voice is selected.
                Box(
                    modifier = Modifier.size(CHECKMARK_SIZE),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check_40dp),
                            contentDescription = stringResource(R.string.selected),
                            modifier = Modifier.size(CHECKMARK_SIZE)
                        )
                    }
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

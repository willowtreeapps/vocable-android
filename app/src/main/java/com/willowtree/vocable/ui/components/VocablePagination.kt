package com.willowtree.vocable.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.theme.VocableTheme

/**
 * Shared "Page X of Y" pager, mirroring iOS's `PaginationView`: a previous button, the page label
 * and a next button as one horizontally centered cluster, with both buttons disabled when there is
 * only a single page.
 *
 * Paging wraps in both directions. iOS's carousel repeats its content 100 times and starts the user
 * mid-list, so there is no reachable first or last page there either.
 *
 * Only [com.willowtree.vocable.ui.voiceselection.VoiceSelectionScreen] uses this so far — Presets,
 * EditCategories and EditCategoryPhrases each still carry their own inline copy of this row and
 * should be migrated onto it.
 */
@Composable
fun VocablePagination(
    pageIndex: Int,
    pageCount: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    buttonSize: Dp,
    modifier: Modifier = Modifier,
    // Defaults to the arrow drawables' own 40dp intrinsic size, so existing callers are unaffected.
    // Callers with per-breakpoint button sizes should pass a matching icon size, or the arrow reads
    // as undersized in the larger buckets and overfull in the smallest.
    iconSize: Dp = 40.dp
) {
    // Matches iOS's setPaginationButtonsEnabled(pageCount > 1) — a lone page has nowhere to go.
    val enabled = pageCount > 1

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GazeButton(
            onClick = onPreviousPage,
            enabled = enabled,
            accessibilityLabel = stringResource(R.string.previous_page),
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                painter = painterResource(
                    id = if (enabled) {
                        R.drawable.ic_phrases_arrow_back_blue
                    } else {
                        R.drawable.ic_phrases_arrow_back_disabled
                    }
                ),
                contentDescription = stringResource(R.string.previous_page),
                // The arrow drawables carry their own colors, so they must not be tinted.
                tint = Color.Unspecified,
                modifier = Modifier.size(iconSize)
            )
        }

        Text(
            text = stringResource(R.string.phrases_page_number, pageIndex + 1, pageCount),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        GazeButton(
            onClick = onNextPage,
            enabled = enabled,
            accessibilityLabel = stringResource(R.string.next_page),
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                painter = painterResource(
                    id = if (enabled) {
                        R.drawable.ic_phrases_arrow_forward_blue
                    } else {
                        R.drawable.ic_phrases_arrow_forward_disabled
                    }
                ),
                contentDescription = stringResource(R.string.next_page),
                tint = Color.Unspecified,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Preview
@Composable
private fun VocablePaginationPreview() {
    VocableTheme {
        VocablePagination(
            pageIndex = 0,
            pageCount = 3,
            onPreviousPage = {},
            onNextPage = {},
            buttonSize = 64.dp
        )
    }
}

@Preview
@Composable
private fun VocablePaginationSinglePagePreview() {
    VocableTheme {
        VocablePagination(
            pageIndex = 0,
            pageCount = 1,
            onPreviousPage = {},
            onNextPage = {},
            buttonSize = 64.dp
        )
    }
}

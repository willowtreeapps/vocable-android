package com.willowtree.vocable.ui.resetsettings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.base.MviScreen
import com.willowtree.vocable.ui.components.ConfirmationDialog
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.theme.ColorPrimary
import com.willowtree.vocable.ui.theme.ColorPrimaryDark
import com.willowtree.vocable.ui.theme.ErrorColor
import com.willowtree.vocable.ui.theme.SelectedColor
import com.willowtree.vocable.ui.theme.TextColor
import com.willowtree.vocable.ui.theme.VocableTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun ResetSettingsScreen(
    onBack: () -> Unit,
    viewModel: ResetSettingsViewModel = koinViewModel()
) {
    MviScreen(viewModel = viewModel, onEvent = { event ->
        when (event) {
            ResetSettingsEvent.NavigateBack -> onBack()
        }
    }) { state ->
        ResetSettingsContent(
            state = state,
            onBack = viewModel::onBack,
            onToggleDomain = viewModel::toggleDomain,
            onRequestResetSelected = viewModel::requestResetSelected,
            onRequestResetEverything = viewModel::requestResetEverything,
            onDismissDialog = viewModel::dismissDialog,
            onConfirmDialog = viewModel::confirmDialog,
            onNextPage = viewModel::nextPage,
            onPrevPage = viewModel::prevPage,
            onUpdateItemsPerPage = viewModel::updateItemsPerPage
        )
    }
}

private data class ResetDomainInfo(
    val domain: ResetDomain,
    val label: Int
)

private val RESET_DOMAINS = listOf(
    ResetDomainInfo(ResetDomain.VOICE, R.string.settings_options_voice),
    ResetDomainInfo(ResetDomain.SENSITIVITY, R.string.timing_sensitivity_title),
    ResetDomainInfo(ResetDomain.SELECTION_MODE, R.string.settings_selection_mode),
    ResetDomainInfo(ResetDomain.CATEGORIES, R.string.categories_edit_title),
    ResetDomainInfo(ResetDomain.PHRASES, R.string.reset_phrases_label)
)

@Composable
private fun ResetSettingsContent(
    state: ResetSettingsState,
    onBack: () -> Unit,
    onToggleDomain: (ResetDomain) -> Unit,
    onRequestResetSelected: () -> Unit,
    onRequestResetEverything: () -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmDialog: () -> Unit,
    onNextPage: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    onUpdateItemsPerPage: (Int) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val density = LocalDensity.current

    val rootMargin = dimensionResource(id = R.dimen.settings_margin_default)
    val rowHeight = 80.dp
    val rowSpacing = 12.dp
    val headerBottomMargin = 24.dp
    val actionsTopMargin = 16.dp
    val pageControlTopMargin = 16.dp
    val pagingButtonSize = dimensionResource(id = R.dimen.edit_paging_button_width)
    val maxColumns = 3

    val rootMarginPx = with(density) { rootMargin.roundToPx() }
    val rowHeightPx = with(density) { rowHeight.roundToPx() }
    val rowSpacingPx = with(density) { rowSpacing.roundToPx() }
    val headerBottomMarginPx = with(density) { headerBottomMargin.roundToPx() }
    val actionsTopMarginPx = with(density) { actionsTopMargin.roundToPx() }
    val pageControlTopMarginPx = with(density) { pageControlTopMargin.roundToPx() }
    val pagingButtonSizePx = with(density) { pagingButtonSize.roundToPx() }

    var rootHeightPx by remember { mutableIntStateOf(0) }
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var actionsHeightPx by remember { mutableIntStateOf(0) }
    // One full-width button per row is the default/preferred layout; only step up to 2 or 3
    // columns when there isn't enough vertical room for that.
    var columns by remember { mutableIntStateOf(1) }

    LaunchedEffect(rootHeightPx, headerHeightPx, actionsHeightPx) {
        if (rootHeightPx == 0 || headerHeightPx == 0 || actionsHeightPx == 0) return@LaunchedEffect

        val domainCount = ResetDomain.entries.size
        val availableListHeight = rootHeightPx - headerHeightPx - headerBottomMarginPx -
            actionsHeightPx - actionsTopMarginPx

        fun heightForRows(rows: Int): Int = (rows * rowHeightPx) + ((rows - 1).coerceAtLeast(0) * rowSpacingPx)

        // Try 1 column first, then 2, then 3 - the smallest column count where every domain still
        // fits on one page without paging.
        val fittingColumns = (1..maxColumns).firstOrNull { cols ->
            val rows = ceil(domainCount.toFloat() / cols).toInt()
            heightForRows(rows) <= availableListHeight
        }

        if (fittingColumns != null) {
            if (columns != fittingColumns) columns = fittingColumns
            if (state.itemsPerPage != domainCount) onUpdateItemsPerPage(domainCount)
        } else {
            // Even the densest grid (3 columns) doesn't fit everything on one page - use it anyway
            // and fall back to measured pagination for whatever doesn't fit.
            if (columns != maxColumns) columns = maxColumns
            val availableForPaging = availableListHeight - pagingButtonSizePx - pageControlTopMarginPx
            val rowsThatFit = if (availableForPaging < rowHeightPx) {
                1
            } else {
                floor(
                    (availableForPaging + rowSpacingPx).toFloat() / (rowHeightPx + rowSpacingPx).toFloat()
                ).toInt().coerceAtLeast(1)
            }
            val newItemsPerPage = (rowsThatFit * maxColumns).coerceAtMost(domainCount)
            if (newItemsPerPage != state.itemsPerPage) onUpdateItemsPerPage(newItemsPerPage)
        }
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(rootMargin)
            .onSizeChanged { rootHeightPx = it.height - (2 * rootMarginPx) }
    ) {
        val (headerRef, listRef, pageControlRef, actionsRef) = createRefs()
        val backButtonSize = dimensionResource(id = R.dimen.settings_close_button_width)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(headerRef) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .onSizeChanged { headerHeightPx = it.height }
        ) {
            GazeButton(
                onClick = onBack,
                modifier = Modifier.size(backButtonSize)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_40dp),
                    contentDescription = stringResource(R.string.close_settings),
                    tint = Color.Unspecified
                )
            }

            Text(
                text = stringResource(id = R.string.settings_reset_app),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    fontSize = dimensionResource(id = R.dimen.settings_title_text_size).value.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            )
        }

        val pageDomains = RESET_DOMAINS
            .chunked(state.itemsPerPage.coerceAtLeast(1))
            .getOrElse(state.currentPage) { emptyList() }

        Column(
            modifier = Modifier.constrainAs(listRef) {
                top.linkTo(headerRef.bottom, margin = headerBottomMargin)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            verticalArrangement = Arrangement.spacedBy(rowSpacing)
        ) {
            pageDomains.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { info ->
                        ResetDomainRow(
                            label = stringResource(id = info.label),
                            checked = info.domain in state.checkedDomains,
                            onToggle = { onToggleDomain(info.domain) },
                            singleColumn = columns == 1,
                            modifier = Modifier
                                .weight(1f)
                                .height(rowHeight)
                        )
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (state.totalPages > 1) {
            Row(
                modifier = Modifier
                    .constrainAs(pageControlRef) {
                        top.linkTo(listRef.bottom, margin = pageControlTopMargin)
                        centerHorizontallyTo(parent)
                    }
                    .testTag("reset_settings_page_control"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GazeButton(
                    onClick = onPrevPage,
                    modifier = Modifier.size(pagingButtonSize)
                ) {
                    Icon(painterResource(id = R.drawable.ic_phrases_arrow_back_blue), null, tint = Color.Unspecified)
                }

                Text(
                    text = stringResource(R.string.phrases_page_number, state.currentPage + 1, state.totalPages),
                    color = TextColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = dimensionResource(id = R.dimen.phrases_page_number_text_size).value.sp
                )

                GazeButton(
                    onClick = onNextPage,
                    modifier = Modifier.size(pagingButtonSize)
                ) {
                    Icon(painterResource(id = R.drawable.ic_phrases_arrow_forward_blue), null, tint = Color.Unspecified)
                }
            }
        }

        val actionButtonHeight = dimensionResource(id = R.dimen.selection_mode_button_height)
        val actionsTopAnchor = if (state.totalPages > 1) pageControlRef.bottom else listRef.bottom

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .constrainAs(actionsRef) {
                        top.linkTo(actionsTopAnchor, margin = actionsTopMargin)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
                    .onSizeChanged { actionsHeightPx = it.height },
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ResetSelectedButton(
                    enabled = state.checkedDomains.isNotEmpty(),
                    onClick = onRequestResetSelected,
                    height = actionButtonHeight,
                    modifier = Modifier.weight(1f)
                )
                ResetEverythingButton(
                    onClick = onRequestResetEverything,
                    height = actionButtonHeight,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .constrainAs(actionsRef) {
                        top.linkTo(actionsTopAnchor, margin = actionsTopMargin)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
                    .onSizeChanged { actionsHeightPx = it.height },
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ResetSelectedButton(
                    enabled = state.checkedDomains.isNotEmpty(),
                    onClick = onRequestResetSelected,
                    height = actionButtonHeight,
                    modifier = Modifier.fillMaxWidth()
                )
                ResetEverythingButton(
                    onClick = onRequestResetEverything,
                    height = actionButtonHeight,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (state.dialogTarget != null) {
        val (title, message) = when (state.dialogTarget) {
            ResetDialogTarget.Everything ->
                stringResource(R.string.reset_settings_reset_everything) to stringResource(R.string.settings_reset_dialog_message)

            ResetDialogTarget.Selected ->
                stringResource(R.string.reset_settings_selected_dialog_title) to stringResource(R.string.reset_settings_selected_dialog_message)
        }
        ConfirmationDialog(
            title = title,
            message = message,
            confirmText = stringResource(R.string.settings_reset_dialog_confirm),
            onDismiss = onDismissDialog,
            onConfirm = onConfirmDialog,
            isDestructive = true
        )
    }
}

@Composable
private fun ResetSelectedButton(
    enabled: Boolean,
    onClick: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier
) {
    GazeButton(
        onClick = onClick,
        enabled = enabled,
        backgroundColor = ColorPrimary,
        textColor = TextColor,
        modifier = modifier.height(height)
    ) {
        Text(
            text = stringResource(R.string.reset_settings_reset_selected),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ResetEverythingButton(
    onClick: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier
) {
    GazeButton(
        onClick = onClick,
        backgroundColor = ErrorColor,
        textColor = TextColor,
        modifier = modifier
            .height(height)
            .testTag("reset_settings_everything_button")
    ) {
        Text(
            text = stringResource(R.string.reset_settings_reset_everything),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ResetDomainRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    singleColumn: Boolean,
    modifier: Modifier = Modifier
) {
    // Selected state reads as a gaze-button toggle (full background swap, like SensitivityButton's
    // low/medium/high picker) rather than a small Material checkbox off to the side - a bigger,
    // higher-contrast target for gaze/motor-impaired users. No description here - what a reset
    // does is already spelled out in the confirmation dialog, so the row only needs a label.
    val backgroundColor = if (checked) SelectedColor else ColorPrimary
    val contentColor = if (checked) ColorPrimaryDark else TextColor

    GazeButton(
        onClick = onToggle,
        backgroundColor = backgroundColor,
        textColor = contentColor,
        modifier = modifier
    ) {
        if (singleColumn) {
            // Full-width row: label and bullet side by side reads more naturally than stacked.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                ResetSelectionBullet(checked = checked, contentColor = contentColor)
            }
        } else {
            // Narrower multi-column cell: stack a shrink-to-fit label above the bullet instead.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BasicText(
                    text = label,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        textAlign = TextAlign.Center
                    ),
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 11.sp,
                        maxFontSize = 18.sp,
                        stepSize = 0.5.sp
                    ),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                ResetSelectionBullet(checked = checked, contentColor = contentColor)
            }
        }
    }
}

/**
 * A filled/outlined selection dot standing in for a checkmark - decorative only (the whole row is
 * the gaze target via [GazeButton] above it).
 */
@Composable
private fun ResetSelectionBullet(
    checked: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .border(width = 2.dp, color = contentColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color = contentColor, shape = CircleShape)
            )
        }
    }
}

@Preview(name = "Portrait", showBackground = true)
@Preview(name = "Landscape", showBackground = true, widthDp = 768, heightDp = 480)
@Preview(name = "Short landscape (forces pagination)", showBackground = true, widthDp = 800, heightDp = 260)
@Composable
private fun ResetSettingsScreenPreview() {
    VocableTheme {
        ResetSettingsContent(
            state = ResetSettingsState(checkedDomains = setOf(ResetDomain.VOICE)),
            onBack = {},
            onToggleDomain = {},
            onRequestResetSelected = {},
            onRequestResetEverything = {},
            onDismissDialog = {},
            onConfirmDialog = {}
        )
    }
}

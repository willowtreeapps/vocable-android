package com.willowtree.vocable.ui.editphrases

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.willowtree.vocable.R
import com.willowtree.vocable.core.locale.LocalesWithText
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.domain.model.CustomPhrase
import com.willowtree.vocable.domain.model.Phrase
import com.willowtree.vocable.ui.base.MviScreen
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.theme.ColorPrimary
import com.willowtree.vocable.ui.theme.TextColor
import com.willowtree.vocable.ui.theme.VocableTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.floor

/**
 * Displays the list of phrases for a category, matching wireframe 1 (single column list).
 * Each row has a Trash icon on the left, phrase text in the center, and a chevron on the right.
 */
@Composable
fun EditCategoryPhrasesScreen(
    categoryId: String,
    onBack: () -> Unit,
    onAddPhrase: (String) -> Unit,
    onEditPhrase: (String, String) -> Unit,
    viewModel: EditCategoryPhrasesViewModel = koinViewModel()
) {
    LaunchedEffect(categoryId) { viewModel.loadCategory(categoryId) }

    MviScreen(viewModel = viewModel, onEvent = { event ->
        when (event) {
            EditCategoryPhrasesEvent.NavigateBack -> onBack()
            is EditCategoryPhrasesEvent.NavigateToAddPhrase -> onAddPhrase(event.categoryId)
            is EditCategoryPhrasesEvent.NavigateToEditPhrase -> onEditPhrase(
                event.phraseId,
                event.text
            )
        }
    }) { state ->
        EditCategoryPhrasesContent(
            state = state,
            onIntent = viewModel::onIntent
        )
    }
}

@Composable
private fun EditCategoryPhrasesContent(
    state: EditCategoryPhrasesState,
    onIntent: (EditCategoryPhrasesIntent) -> Unit
) {
    val isCustomCategory = state.category is Category.StoredCategory
    val density = LocalDensity.current

    val topPadding = dimensionResource(id = R.dimen.main_activity_top_bottom_margin)
    val actionButtonSize = dimensionResource(id = R.dimen.edit_categories_action_button_width)
    val horizontalMargin = 24.dp
    val listTopMargin = 32.dp
    val listBottomMargin = 16.dp
    val pageControlBottomMargin = 32.dp
    val rowHeight = 56.dp
    val rowSpacing = 8.dp

    val rowHeightPx = with(density) { rowHeight.roundToPx() }
    val rowSpacingPx = with(density) { rowSpacing.roundToPx() }
    val listTopMarginPx = with(density) { listTopMargin.roundToPx() }
    val listBottomMarginPx = with(density) { listBottomMargin.roundToPx() }
    val pageControlBottomMarginPx = with(density) { pageControlBottomMargin.roundToPx() }
    val topPaddingPx = with(density) { topPadding.roundToPx() }

    var rootHeightPx by remember { mutableIntStateOf(0) }
    var titleHeightPx by remember { mutableIntStateOf(0) }
    var pageControlHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(rootHeightPx, titleHeightPx, pageControlHeightPx, rowHeightPx, rowSpacingPx) {
        if (rootHeightPx == 0 || titleHeightPx == 0 || pageControlHeightPx == 0) return@LaunchedEffect

        val availableListHeight = rootHeightPx -
            titleHeightPx -
            listTopMarginPx -
            listBottomMarginPx -
            pageControlHeightPx -
            pageControlBottomMarginPx

        val rowsThatFit = if (availableListHeight < rowHeightPx) {
            1
        } else {
            floor(
                (availableListHeight + rowSpacingPx).toFloat() / (rowHeightPx + rowSpacingPx).toFloat()
            ).toInt().coerceAtLeast(1)
        }

        if (rowsThatFit != state.itemsPerPage) {
            onIntent(EditCategoryPhrasesIntent.UpdateItemsPerPage(rowsThatFit))
        }
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding)
            .onSizeChanged { rootHeightPx = it.height - topPaddingPx }
    ) {
        val (backRef, titleRef, addRef, listRef, pageControlRef, emptyRef) = createRefs()

        GazeButton(
            onClick = { onIntent(EditCategoryPhrasesIntent.Back) },
            modifier = Modifier
                .size(actionButtonSize)
                .constrainAs(backRef) {
                    top.linkTo(titleRef.top)
                    bottom.linkTo(titleRef.bottom)
                    start.linkTo(parent.start, margin = horizontalMargin)
                }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back_40dp),
                contentDescription = stringResource(R.string.close_settings),
                tint = Color.Unspecified
            )
        }

        BasicText(
            text = state.categoryName,
            style = TextStyle(
                fontSize = dimensionResource(id = R.dimen.spoken_text_view_text_size).value.sp,
                textAlign = TextAlign.Start,
                color = TextColor,
                fontWeight = FontWeight.SemiBold
            ),
            autoSize = TextAutoSize.StepBased(
                minFontSize = 10.sp,
                maxFontSize = dimensionResource(id = R.dimen.spoken_text_view_text_size).value.sp,
                stepSize = 0.5.sp
            ),
            maxLines = 1,
            minLines = 1,
            modifier = Modifier
                .constrainAs(titleRef) {
                    top.linkTo(parent.top)
                    centerHorizontallyTo(parent)
                }
                .onSizeChanged { titleHeightPx = it.height }
        )

        GazeButton(
            onClick = { onIntent(EditCategoryPhrasesIntent.AddPhrase) },
            modifier = Modifier
                .size(actionButtonSize)
                .constrainAs(addRef) {
                    top.linkTo(titleRef.top)
                    bottom.linkTo(titleRef.bottom)
                    end.linkTo(parent.end, margin = horizontalMargin)
                }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_40dp),
                contentDescription = stringResource(R.string.add_phrase),
                tint = Color.Unspecified
            )
        }

        if (state.phrases.isEmpty()) {
            Column(
                modifier = Modifier.constrainAs(emptyRef) {
                    centerTo(parent)
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.custom_category_empty),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextColor,
                    fontWeight = FontWeight.Bold
                )
                if (isCustomCategory) {
                    Spacer(modifier = Modifier.height(16.dp))
                    GazeButton(onClick = { onIntent(EditCategoryPhrasesIntent.AddPhrase) }) {
                        Text(
                            text = stringResource(R.string.add_phrase),
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.constrainAs(listRef) {
                    top.linkTo(titleRef.bottom, margin = listTopMargin)
                    bottom.linkTo(pageControlRef.top, margin = listBottomMargin)
                    start.linkTo(parent.start, margin = horizontalMargin)
                    end.linkTo(parent.end, margin = horizontalMargin)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                },
                verticalArrangement = Arrangement.spacedBy(rowSpacing)
            ) {
                val pageItems = state.currentPagePhrases

                repeat(state.itemsPerPage) { i ->
                    val phrase = pageItems.getOrNull(i)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                    ) {
                        if (phrase != null) {
                            PhraseEditItem(
                                phrase = phrase,
                                onEdit = { text ->
                                    onIntent(
                                        EditCategoryPhrasesIntent.EditPhrase(
                                            phrase.phraseId,
                                            text
                                        )
                                    )
                                },
                                onDelete = { onIntent(EditCategoryPhrasesIntent.DeletePhrase(phrase.phraseId)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .constrainAs(pageControlRef) {
                        bottom.linkTo(parent.bottom, margin = pageControlBottomMargin)
                        centerHorizontallyTo(parent)
                    }
                    .onSizeChanged { pageControlHeightPx = it.height },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val pagingButtonSize = dimensionResource(id = R.dimen.edit_paging_button_width)

                GazeButton(
                    onClick = { onIntent(EditCategoryPhrasesIntent.PrevPage) },
                    modifier = Modifier.size(pagingButtonSize)
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_phrases_arrow_back_blue),
                        null,
                        tint = Color.Unspecified
                    )
                }

                Text(
                    text = stringResource(
                        R.string.phrases_page_number,
                        state.currentPage + 1,
                        state.totalPages
                    ),
                    color = TextColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = dimensionResource(id = R.dimen.phrases_page_number_text_size).value.sp
                )

                GazeButton(
                    onClick = { onIntent(EditCategoryPhrasesIntent.NextPage) },
                    modifier = Modifier.size(pagingButtonSize)
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_phrases_arrow_forward_blue),
                        null,
                        tint = Color.Unspecified
                    )
                }
            }
        }
    }
}

@Composable
private fun PhraseEditItem(
    phrase: Phrase,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val phraseText = phrase.text(context)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GazeButton(
            onClick = onDelete,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = stringResource(R.string.keyboard_clear),
                tint = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }

        GazeButton(
            onClick = { onEdit(phraseText) },
            backgroundColor = ColorPrimary,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = phraseText,
                    color = TextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    maxLines = 1
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right_32dp),
                    contentDescription = stringResource(R.string.edit_phrases),
                    tint = Color.Unspecified,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = false, device = Devices.PIXEL)
@Preview(
    name = "Landscape",
    device = "spec:width=800dp,height=400dp,dpi=240",
    showBackground = true,
)
@Preview(showBackground = false, device = Devices.TABLET)
@Composable
private fun EditCategoryPhrasesPreview() {
    VocableTheme {
        EditCategoryPhrasesContent(
            state = EditCategoryPhrasesState(
                category = Category.StoredCategory(
                    "1",
                    LocalesWithText(mapOf("en" to "General")), false, 0
                ),
                categoryName = "General",
                phrases = listOf(
                    CustomPhrase("1", 0, LocalesWithText(mapOf("en" to "Please")), null),
                    CustomPhrase("2", 1, LocalesWithText(mapOf("en" to "Thank you")), null),
                    CustomPhrase("3", 2, LocalesWithText(mapOf("en" to "Yes")), null),
                    CustomPhrase("4", 3, LocalesWithText(mapOf("en" to "No")), null),
                    CustomPhrase("5", 4, LocalesWithText(mapOf("en" to "Maybe")), null),
                    CustomPhrase("6", 5, LocalesWithText(mapOf("en" to "Please wait")), null),
                    CustomPhrase("7", 6, LocalesWithText(mapOf("en" to "I don't know")), null),
                    CustomPhrase(
                        "8", 7,
                        LocalesWithText(mapOf("en" to "I didn't mean to say ...")), null
                    ),
                )
            ),
            onIntent = {}
        )
    }
}

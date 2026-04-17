package com.willowtree.vocable.ui.editcategories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.willowtree.vocable.R
import com.willowtree.vocable.core.locale.LocalesWithText
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.ui.base.MviScreen
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.theme.TextColor
import com.willowtree.vocable.ui.theme.VocableTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.floor

@Composable
fun EditCategoriesScreen(
    onBack: () -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (Category) -> Unit,
    viewModel: EditCategoriesViewModel = koinViewModel()
) {
    MviScreen(viewModel = viewModel, onEvent = { event ->
        when (event) {
            EditCategoriesEvent.NavigateBack -> onBack()
            EditCategoriesEvent.NavigateToAddCategory -> onAddCategory()
            is EditCategoriesEvent.NavigateToEditCategory -> onEditCategory(event.category)
        }
    }) { state ->
        EditCategoriesContent(
            state = state,
            onIntent = viewModel::onIntent
        )
    }
}

@Composable
private fun EditCategoriesContent(
    state: EditCategoriesState,
    onIntent: (EditCategoriesIntent) -> Unit
) {
    val topPadding = dimensionResource(id = R.dimen.main_activity_top_bottom_margin)
    val horizontalMargin = 24.dp
    val listTopMargin = 32.dp
    val listBottomMargin = 16.dp
    val pageControlBottomMargin = 32.dp
    val rowHeight = dimensionResource(R.dimen.edit_categories_list_row_height)
    val rowSpacing = 2.dp
    val pagingButtonSize = dimensionResource(id = R.dimen.edit_paging_button_width)
    val density = LocalDensity.current

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
            onIntent(EditCategoriesIntent.UpdateItemsPerPage(rowsThatFit))
        }
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding)
            .onSizeChanged { rootHeightPx = it.height - topPaddingPx }
    ) {
        val (titleRef, backButtonRef, addButtonRef, listRef, pageControlRef) = createRefs()

        Text(
            text = stringResource(id = R.string.categories_edit_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextColor,
                fontSize = dimensionResource(id = R.dimen.edit_categories_title_text_size).value.sp
            ),
            modifier = Modifier
                .constrainAs(titleRef) {
                    top.linkTo(parent.top)
                    centerHorizontallyTo(parent)
                }
                .onSizeChanged { titleHeightPx = it.height }
        )

        GazeButton(
            onClick = { onIntent(EditCategoriesIntent.Back) },
            accessibilityLabel = stringResource(R.string.close_settings),
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.edit_categories_action_button_width))
                .constrainAs(backButtonRef) {
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

        GazeButton(
            onClick = { onIntent(EditCategoriesIntent.AddCategory) },
            accessibilityLabel = stringResource(R.string.add_category),
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.edit_categories_action_button_width))
                .constrainAs(addButtonRef) {
                    top.linkTo(titleRef.top)
                    bottom.linkTo(titleRef.bottom)
                    end.linkTo(parent.end, margin = horizontalMargin)
                }
                .testTag("edit_categories_add_button")
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_40dp),
                contentDescription = stringResource(R.string.add_category),
                tint = Color.Unspecified
            )
        }

        Column(
            modifier = Modifier.constrainAs(listRef) {
                top.linkTo(backButtonRef.bottom, margin = listTopMargin)
                bottom.linkTo(pageControlRef.top, margin = listBottomMargin)
                start.linkTo(parent.start, margin = horizontalMargin)
                end.linkTo(parent.end, margin = horizontalMargin)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            },
            verticalArrangement = Arrangement.spacedBy(rowSpacing)
        ) {
            repeat(state.itemsPerPage) { i ->
                val category = state.currentPageCategories.getOrNull(i)
                val overallIndex = (state.currentPage * state.itemsPerPage) + i

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                ) {
                    if (category != null) {
                        EditCategoryItem(
                            category = category,
                            canMoveUp = !category.hidden && overallIndex > 0,
                            canMoveDown = !category.hidden && overallIndex + 1 < state.visibleCount,
                            onEdit = { onIntent(EditCategoriesIntent.EditCategory(category)) },
                            onMoveUp = { onIntent(EditCategoriesIntent.MoveCategoryUp(category.categoryId)) },
                            onMoveDown = { onIntent(EditCategoriesIntent.MoveCategoryDown(category.categoryId)) }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .constrainAs(pageControlRef) {
                    top.linkTo(listRef.bottom)
                    bottom.linkTo(parent.bottom, margin = pageControlBottomMargin)
                    centerHorizontallyTo(parent)
                }
                .onSizeChanged { pageControlHeightPx = it.height },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GazeButton(
                onClick = { onIntent(EditCategoriesIntent.PrevPage) },
                modifier = Modifier
                    .size(pagingButtonSize)
                    .testTag("edit_categories_prev_page")
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
                fontSize = dimensionResource(id = R.dimen.phrases_page_number_text_size).value.sp,
                modifier = Modifier.testTag("edit_categories_page_indicator")
            )

            GazeButton(
                onClick = { onIntent(EditCategoriesIntent.NextPage) },
                modifier = Modifier
                    .size(pagingButtonSize)
                    .testTag("edit_categories_next_page")
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

@Composable
private fun EditCategoryItem(
    category: Category,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.edit_categories_list_arrow_margin))
        ) {
            GazeButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier
                    .testTag("edit_category_move_up_${category.categoryId}")
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_up_arrow),
                    null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            }

            GazeButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier
                    .testTag("edit_category_move_down_${category.categoryId}")
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_down_arrow),
                    null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            }

            GazeButton(
                onClick = onEdit,
                accessibilityLabel = category.text(context),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("edit_category_item_${category.categoryId}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = category.text(context),
                        color = TextColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = dimensionResource(id = R.dimen.settings_button_text_size).value.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    Icon(
                        painterResource(id = R.drawable.ic_arrow_right_32dp),
                        null,
                        tint = Color.Unspecified
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Preview(showBackground = true, device = Devices.TABLET)
@Composable
private fun EditCategoriesPreview() {
    VocableTheme {
        EditCategoriesContent(
            state = EditCategoriesState(
                categories = listOf(
                    Category.StoredCategory(
                        "1",
                        LocalesWithText(mapOf("en" to "General")),
                        false,
                        0
                    ),
                    Category.StoredCategory("2", LocalesWithText(mapOf("en" to "Needs")), false, 1),
                    Category.StoredCategory("3", LocalesWithText(mapOf("en" to "Needs1")), false, 2),
                    Category.StoredCategory("4", LocalesWithText(mapOf("en" to "Needs2")), false, 3),
                    Category.StoredCategory("5", LocalesWithText(mapOf("en" to "Needs3")), false, 4),
                    Category.StoredCategory("6", LocalesWithText(mapOf("en" to "Needs4")), false, 5),
                )
            ),
            onIntent = {}
        )
    }
}

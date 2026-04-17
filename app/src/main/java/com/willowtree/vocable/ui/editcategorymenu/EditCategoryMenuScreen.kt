package com.willowtree.vocable.ui.editcategorymenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.willowtree.vocable.ui.base.MviScreen
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.theme.ColorPrimary
import com.willowtree.vocable.ui.theme.ErrorColor
import com.willowtree.vocable.ui.theme.SelectedColor
import com.willowtree.vocable.ui.theme.TextColor
import com.willowtree.vocable.ui.theme.VocableTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.floor

@Composable
fun EditCategoryMenuScreen(
    categoryId: String,
    onBack: () -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onEditPhrases: (String) -> Unit,
    viewModel: EditCategoryMenuViewModel = koinViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(categoryId) { viewModel.loadCategory(categoryId) }

    MviScreen(viewModel = viewModel, onEvent = { event ->
        when (event) {
            EditCategoryMenuEvent.NavigateBack -> onBack()
            is EditCategoryMenuEvent.NavigateToRenameCategory -> {
                val categoryName = viewModel.uiState.value.category?.text(context).orEmpty()
                onRenameCategory(event.categoryId, categoryName)
            }
            is EditCategoryMenuEvent.NavigateToEditPhrases -> onEditPhrases(event.categoryId)
        }
    }) { state ->
        EditCategoryMenuContent(state = state, onIntent = viewModel::onIntent)
    }
}

@Composable
private fun EditCategoryMenuContent(
    state: EditCategoryMenuState,
    onIntent: (EditCategoryMenuIntent) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val category = state.category
    val categoryName = category?.text(context) ?: ""

    val horizontalPadding = 24.dp
    val verticalPadding = 32.dp
    val headerBottomSpacing = 24.dp
    val itemSpacing = 16.dp
    val actionButtonHeight = 40.dp

    val headerBottomSpacingPx = with(density) { headerBottomSpacing.roundToPx() }
    val itemSpacingPx = with(density) { itemSpacing.roundToPx() }
    val actionButtonHeightPx = with(density) { actionButtonHeight.roundToPx() }

    var availableHeightPx by remember { mutableIntStateOf(0) }
    var headerHeightPx by remember { mutableIntStateOf(0) }

    val visibleActionCount = remember(
        availableHeightPx,
        headerHeightPx,
        headerBottomSpacingPx,
        itemSpacingPx,
        actionButtonHeightPx
    ) {
        if (availableHeightPx == 0 || headerHeightPx == 0) {
            4
        } else {
            val availableActionsHeight = availableHeightPx - headerHeightPx - headerBottomSpacingPx
            val rowsThatFit = floor(
                (availableActionsHeight + itemSpacingPx).toFloat() / (actionButtonHeightPx + itemSpacingPx).toFloat()
            ).toInt()
            rowsThatFit.coerceIn(1, 4)
        }
    }

    val showRename = visibleActionCount >= 1
    val showToggle = visibleActionCount >= 2
    val showEditPhrases = visibleActionCount >= 3
    val showRemove = visibleActionCount >= 4

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .onSizeChanged { availableHeightPx = it.height }
    ) {
        val (headerRef, actionsRef) = createRefs()

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
                onClick = { onIntent(EditCategoryMenuIntent.Back) },
                modifier = Modifier.size(dimensionResource(id = R.dimen.edit_categories_action_button_width))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_40dp),
                    contentDescription = stringResource(R.string.close_settings),
                    tint = Color.Unspecified
                )
            }

            Text(
                text = categoryName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .fillMaxWidth()
            )
        }

        Column(
            modifier = Modifier.constrainAs(actionsRef) {
                top.linkTo(headerRef.bottom, margin = headerBottomSpacing)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            if (showRename) {
                GazeButton(
                    onClick = { onIntent(EditCategoryMenuIntent.RenameCategory) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(actionButtonHeight)
                ) {
                    Text(
                        text = stringResource(R.string.rename_category),
                        color = TextColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right_32dp),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
            }

            if (showToggle) {
                GazeButton(
                    onClick = { onIntent(EditCategoryMenuIntent.SetCategoryShown(category?.hidden == true)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(actionButtonHeight)
                ) {
                    Text(
                        text = stringResource(R.string.show_category),
                        color = TextColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    )
                    Switch(
                        checked = category?.hidden == false,
                        onCheckedChange = { shown ->
                            onIntent(
                                EditCategoryMenuIntent.SetCategoryShown(
                                    shown
                                )
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SelectedColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = ColorPrimary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (showEditPhrases) {
                GazeButton(
                    onClick = { onIntent(EditCategoryMenuIntent.EditPhrases) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(actionButtonHeight)
                ) {
                    Text(
                        text = stringResource(R.string.edit_phrases),
                        color = TextColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right_32dp),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
            }

            if (showRemove) {
                GazeButton(
                    onClick = { onIntent(EditCategoryMenuIntent.DeleteCategory) },
                    enabled = !state.isLastCategory,
                    backgroundColor = ErrorColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(actionButtonHeight)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.remove_category),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Preview(
    name = "Landscape EditCategoryMenuPreview",
    device = "spec:width=800dp,height=400dp,dpi=240",
    showBackground = true
)
@Preview(showBackground = true, device = Devices.TABLET)
@Composable
private fun EditCategoryMenuPreview() {
    VocableTheme {
        EditCategoryMenuContent(
            state = EditCategoryMenuState(
                category = Category.StoredCategory(
                    "1",
                    LocalesWithText(mapOf("en" to "General")),
                    false,
                    0
                ),
                isLastCategory = false
            ),
            onIntent = {}
        )
    }
}

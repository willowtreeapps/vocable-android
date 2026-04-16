package com.willowtree.vocable.ui.presets

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.willowtree.vocable.R
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.domain.model.PhraseGridItem
import com.willowtree.vocable.domain.model.PresetCategories
import com.willowtree.vocable.ui.components.AddPhraseButton
import com.willowtree.vocable.ui.components.GazeButton
import com.willowtree.vocable.ui.modifiers.horizontalPageSwipe
import com.willowtree.vocable.ui.theme.ColorPrimary
import com.willowtree.vocable.ui.theme.ColorPrimaryDark
import com.willowtree.vocable.ui.theme.SelectedColor
import com.willowtree.vocable.ui.theme.TextColor
import com.willowtree.vocable.ui.theme.VocableTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.ceil

@Composable
fun PresetsScreen(
    onNavigateToKeyboard: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddPhrase: (Category) -> Unit,
    viewModel: PresetsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPhrases()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PresetsContent(
        categories = state.categories,
        selectedCategory = state.selectedCategory,
        currentPhrases = state.currentPhrases,
        isLoadingPhrases = state.selectedCategory?.categoryId != state.currentPhrasesCategoryId,
        isSpeaking = state.isSpeaking,
        activeText = state.activeText,
        onCategorySelected = { viewModel.onIntent(PresetsIntent.OnCategorySelected(it)) },
        onAddPhrase = onAddPhrase,
        onNavigateToKeyboard = onNavigateToKeyboard,
        onNavigateToSettings = onNavigateToSettings,
        onPhraseClick = { phraseId, text ->
            viewModel.onIntent(PresetsIntent.Speak(phraseId, text))
        }
    )
}

@Composable
private fun PresetsContent(
    categories: List<Category>,
    selectedCategory: Category?,
    currentPhrases: List<PhraseGridItem>,
    isLoadingPhrases: Boolean,
    isSpeaking: Boolean,
    activeText: String,
    onCategorySelected: (String) -> Unit,
    onAddPhrase: (Category) -> Unit,
    onNavigateToKeyboard: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPhraseClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val useWideLayout = isTablet || isLandscape

    val speechButtonMargin = dimensionResource(id = R.dimen.speech_button_margin)
    val phrasesMargin = dimensionResource(id = R.dimen.phrases_margin)
    val categoryButtonMargin = dimensionResource(id = R.dimen.category_button_margin)

    val phraseColumns = selectedCategory?.categoryId.let { id ->
        if (id == PresetCategories.USER_KEYPAD.id) {
            integerResource(id = R.integer.phrases_columns_one_liner_phrases)
        } else integerResource(id = R.integer.phrases_columns)
    }
    val phraseRows = integerResource(id = R.integer.phrases_rows)
    val maxCategories = integerResource(id = R.integer.max_categories)
    val maxPhrases = selectedCategory?.categoryId.let { id ->
        if (id == PresetCategories.USER_KEYPAD.id) {
            integerResource(id = R.integer.max_phrases_one_liner)
        } else integerResource(id = R.integer.max_phrases)
    }

    var categoryPageIndex by remember { mutableIntStateOf(0) }
    var phrasePageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedCategory) {
        phrasePageIndex = 0
    }

    val isSingleCategoryMode = maxCategories == 1

    LaunchedEffect(selectedCategory, categories, isSingleCategoryMode) {
        if (isSingleCategoryMode && selectedCategory != null) {
            val index = categories.indexOfFirst { it.categoryId == selectedCategory.categoryId }
            if (index != -1) {
                categoryPageIndex = index
            }
        }
    }

    val currentCategoryPageItems =
        remember(categories, categoryPageIndex, maxCategories, isSingleCategoryMode) {
            if (categories.isEmpty()) emptyList()
            else {
                if (isSingleCategoryMode) {
                    listOf(categories[categoryPageIndex % categories.size])
                } else {
                    categories.chunked(maxCategories).getOrElse(categoryPageIndex) { emptyList() }
                }
            }
        }

    val totalCategoryPages = remember(categories, maxCategories) {
        if (categories.isEmpty()) 1 else ceil(categories.size.toFloat() / maxCategories).toInt()
    }

    val actualPhrases = if (isLoadingPhrases) emptyList() else currentPhrases

    val currentPhrasePageItems = remember(actualPhrases, phrasePageIndex, maxPhrases) {
        actualPhrases.chunked(maxPhrases).getOrElse(phrasePageIndex) { emptyList() }
    }

    val totalPhrasePages = remember(actualPhrases, maxPhrases) {
        if (actualPhrases.isEmpty()) 1 else ceil(actualPhrases.size.toFloat() / maxPhrases).toInt()
    }

    fun onNextCategoryPage() {
        if (categories.isEmpty()) return

        if (isSingleCategoryMode) {
            val nextIndex = (categoryPageIndex + 1) % categories.size
            categoryPageIndex = nextIndex
            onCategorySelected(categories[nextIndex].categoryId)
        } else {
            categoryPageIndex = (categoryPageIndex + 1) % totalCategoryPages
        }
    }

    fun onPrevCategoryPage() {
        if (categories.isEmpty()) return

        if (isSingleCategoryMode) {
            val prevIndex =
                if (categoryPageIndex - 1 < 0) categories.size - 1 else categoryPageIndex - 1
            categoryPageIndex = prevIndex
            onCategorySelected(categories[prevIndex].categoryId)
        } else {
            val prevPage =
                if (categoryPageIndex - 1 < 0) totalCategoryPages - 1 else categoryPageIndex - 1
            categoryPageIndex = prevPage
        }
    }

    fun onNextPhrasePage() {
        phrasePageIndex = (phrasePageIndex + 1) % totalPhrasePages
    }

    fun onPrevPhrasePage() {
        phrasePageIndex = if (phrasePageIndex - 1 < 0) totalPhrasePages - 1 else phrasePageIndex - 1
    }

    val isRecents = selectedCategory?.categoryId == PresetCategories.RECENTS.id
    val showEmptyCategories = categories.isEmpty()
    val showEmptyPhrases = !showEmptyCategories && !isLoadingPhrases && currentPhrasePageItems.isEmpty() && !isRecents
    val showNoRecents = !showEmptyCategories && !isLoadingPhrases && actualPhrases.isEmpty() && isRecents

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = dimensionResource(id = R.dimen.main_activity_side_margin),
                end = dimensionResource(id = R.dimen.main_activity_side_margin),
                top = dimensionResource(id = R.dimen.main_activity_top_bottom_margin),
                bottom = dimensionResource(id = R.dimen.main_activity_top_bottom_margin)
            )
            .horizontalPageSwipe(
                onSwipeLeft = { onPrevPhrasePage() },
                onSwipeRight = { onNextPhrasePage() }
            )
    ) {
        val (
            currentTextRef, speakerIconRef, actionButtonsRef,
            categoryBackRef, categoryListRef, categoryForwardRef,
            phrasesListRef,
            phrasesBackRef, pageNumberRef, phrasesForwardRef,
            emptyCategoriesRef, emptyPhrasesRef, emptyAddPhraseButtonRef, noRecentsRef
        ) = createRefs()

        Icon(
            painter = painterResource(id = R.drawable.ic_speaker),
            contentDescription = "Speak",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(48.dp)
                .constrainAs(speakerIconRef) {
                    visibility = if (isSpeaking) Visibility.Visible else Visibility.Gone

                    if (useWideLayout) {
                        end.linkTo(actionButtonsRef.start)
                        top.linkTo(actionButtonsRef.top)
                        bottom.linkTo(actionButtonsRef.bottom)
                    } else {
                        top.linkTo(currentTextRef.top)
                        bottom.linkTo(currentTextRef.bottom)
                    }
                }
        )

        BasicText(
            text = if (isSpeaking) {
                activeText
            } else {
                stringResource(id = R.string.select_something)
            },
            modifier = Modifier.constrainAs(currentTextRef) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                if (useWideLayout) {
                    end.linkTo(speakerIconRef.start, margin = 8.dp)
                } else {
                    end.linkTo(if (isSpeaking) speakerIconRef.start else parent.end, margin = 8.dp)
                }
                width = Dimension.fillToConstraints
            },
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
            maxLines = 2,
            minLines = 2
        )

        if (!useWideLayout && isSpeaking) {
            createHorizontalChain(currentTextRef, speakerIconRef, chainStyle = ChainStyle.Packed)
        }

        Row(
            modifier = Modifier.constrainAs(actionButtonsRef) {
                if (useWideLayout) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                } else {
                    top.linkTo(currentTextRef.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
            },
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.top_bar_button_margin))
        ) {
            val modifier = if (useWideLayout) {
                Modifier.size(dimensionResource(id = R.dimen.top_bar_button_width))
            } else {
                Modifier
                    .weight(1f)
                    .height(dimensionResource(id = R.dimen.top_bar_button_height))
            }

            GazeButton(
                onClick = onNavigateToKeyboard,
                modifier = modifier
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_keyboard),
                    contentDescription = stringResource(R.string.keyboard),
                    tint = Color.Unspecified
                )
            }

            GazeButton(
                onClick = onNavigateToSettings,
                modifier = modifier
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_settings_light_48dp),
                    contentDescription = stringResource(R.string.settings),
                    tint = Color.Unspecified
                )
            }
        }

        if (!showEmptyCategories) {
            val pagingButtonWidth = dimensionResource(id = R.dimen.categories_paging_button_width)
            val pagingButtonHeight = dimensionResource(id = R.dimen.categories_paging_button_height)

            GazeButton(
                onClick = { onPrevCategoryPage() },
                modifier = Modifier
                    .size(width = pagingButtonWidth, height = pagingButtonHeight)
                    .constrainAs(categoryBackRef) {
                        start.linkTo(parent.start)
                        top.linkTo(actionButtonsRef.bottom, margin = 8.dp)
                    }
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_arrow_back_blue),
                    null,
                    tint = Color.Unspecified
                )
            }

            GazeButton(
                onClick = { onNextCategoryPage() },
                modifier = Modifier
                    .size(width = pagingButtonWidth, height = pagingButtonHeight)
                    .constrainAs(categoryForwardRef) {
                        end.linkTo(parent.end)
                        top.linkTo(actionButtonsRef.bottom, margin = 8.dp)
                    }
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_arrow_forward_blue),
                    null,
                    tint = Color.Unspecified
                )
            }

            Row(
                modifier = Modifier.constrainAs(categoryListRef) {
                    top.linkTo(categoryBackRef.top)
                    bottom.linkTo(categoryBackRef.bottom)
                    start.linkTo(categoryBackRef.end, margin = 8.dp)
                    end.linkTo(categoryForwardRef.start, margin = 8.dp)
                    width = Dimension.fillToConstraints
                    height = Dimension.value(pagingButtonHeight)
                },
                horizontalArrangement = Arrangement.spacedBy(categoryButtonMargin)
            ) {
                for (i in 0 until maxCategories) {
                    val category = currentCategoryPageItems.getOrNull(i)
                    if (category != null) {
                        val isSelected = category.categoryId == selectedCategory?.categoryId
                        GazeButton(
                            onClick = { onCategorySelected(category.categoryId) },
                            backgroundColor = if (isSelected) SelectedColor else ColorPrimary,
                            textColor = if (isSelected) ColorPrimaryDark else TextColor,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .fillMaxHeight()
                        ) {
                            BasicText(
                                text = category.text(context),
                                modifier = Modifier.fillMaxWidth(),
                                style = TextStyle(
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) ColorPrimaryDark else TextColor,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 10.sp,
                                    maxFontSize = 20.sp,
                                    stepSize = 0.5.sp
                                ),
                                maxLines = 1,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            Text(
                text = stringResource(R.string.all_categories_hidden_text),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = TextColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(emptyCategoriesRef) {
                    centerTo(parent)
                }
            )
        }

        if (!showEmptyCategories) {
            val pagingButtonWidth = dimensionResource(id = R.dimen.phrases_paging_button_width)
            val pagingButtonHeight = dimensionResource(id = R.dimen.phrases_paging_button_height)

            GazeButton(
                onClick = { onPrevPhrasePage() },
                modifier = Modifier
                    .size(width = pagingButtonWidth, height = pagingButtonHeight)
                    .constrainAs(phrasesBackRef) {
                        bottom.linkTo(parent.bottom)
                        if (!useWideLayout) {
                            start.linkTo(parent.start)
                        }
                    }
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_phrases_arrow_back_blue),
                    null,
                    tint = Color.Unspecified
                )
            }

            BasicText(
                text = stringResource(
                    R.string.phrases_page_number,
                    phrasePageIndex + 1,
                    totalPhrasePages
                ),
                modifier = Modifier.constrainAs(pageNumberRef) {
                    centerVerticallyTo(phrasesBackRef)
                    if (!useWideLayout) {
                        start.linkTo(phrasesBackRef.end)
                        end.linkTo(phrasesForwardRef.start)
                    }
                },
                style = TextStyle(
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    color = TextColor
                ),
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 10.sp,
                    maxFontSize = 20.sp,
                    stepSize = 0.5.sp
                ),
                maxLines = 1,
            )

            GazeButton(
                onClick = { onNextPhrasePage() },
                modifier = Modifier
                    .size(width = pagingButtonWidth, height = pagingButtonHeight)
                    .constrainAs(phrasesForwardRef) {
                        bottom.linkTo(parent.bottom)
                        if (!useWideLayout) {
                            end.linkTo(parent.end)
                        }
                    }
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_phrases_arrow_forward_blue),
                    null,
                    tint = Color.Unspecified
                )
            }

            if (useWideLayout) {
                createHorizontalChain(
                    phrasesBackRef,
                    pageNumberRef,
                    phrasesForwardRef,
                    chainStyle = ChainStyle.SpreadInside
                )
            }
        }

        if (!showEmptyCategories && !showEmptyPhrases && !showNoRecents && !isLoadingPhrases) {
            Column(
                modifier = Modifier.constrainAs(phrasesListRef) {
                    top.linkTo(categoryListRef.bottom, margin = phrasesMargin)
                    bottom.linkTo(phrasesBackRef.top, margin = phrasesMargin)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                },
                verticalArrangement = Arrangement.spacedBy(speechButtonMargin)
            ) {
                for (rowIndex in 0 until phraseRows) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(speechButtonMargin)
                    ) {
                        for (colIndex in 0 until phraseColumns) {
                            val itemIndex = rowIndex * phraseColumns + colIndex
                            val phraseItem = currentPhrasePageItems.getOrNull(itemIndex)

                            if (phraseItem != null) {
                                val modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()

                                when (phraseItem) {
                                    is PhraseGridItem.Phrase -> {
                                        GazeButton(
                                            onClick = {
                                                onPhraseClick(phraseItem.phraseId, phraseItem.text)
                                            },
                                            modifier = modifier.fillMaxWidth()
                                        ) {
                                            BasicText(
                                                text = phraseItem.text,
                                                modifier = Modifier.fillMaxWidth(),
                                                style = TextStyle(
                                                    fontSize = 20.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = TextColor,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                autoSize = TextAutoSize.StepBased(
                                                    minFontSize = 10.sp,
                                                    maxFontSize = 20.sp,
                                                    stepSize = 0.5.sp
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    is PhraseGridItem.AddPhrase -> {
                                        AddPhraseButton(
                                            onClick = { selectedCategory?.let { onAddPhrase(it) } },
                                            modifier = modifier
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else if (showEmptyPhrases) {
            Text(
                text = stringResource(R.string.custom_category_empty),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = TextColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(emptyPhrasesRef) {
                    centerHorizontallyTo(parent)
                    top.linkTo(categoryBackRef.bottom, margin = 32.dp)
                }
            )

            AddPhraseButton(
                onClick = { selectedCategory?.let { onAddPhrase(it) } },
                modifier = Modifier.constrainAs(emptyAddPhraseButtonRef) {
                    top.linkTo(emptyPhrasesRef.bottom, margin = 16.dp)
                    centerHorizontallyTo(parent)
                }
            )

        } else if (showNoRecents) {
            Text(
                text = stringResource(R.string.no_recent_phrases_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = TextColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(noRecentsRef) {
                    centerHorizontallyTo(parent)
                    top.linkTo(categoryBackRef.bottom, margin = 32.dp)
                }
            )
        }
    }
}

@Preview(
    name = "Portrait",
    device = Devices.PIXEL_3,
    showBackground = true,
)
@Preview(
    name = "Landscape",
    device = "spec:width=800dp,height=400dp,dpi=240",
    showBackground = true,
)
@Composable
fun PresetsScreenPreview() {
    VocableTheme {
        PresetsContent(
            categories = listOf(
                Category.PresetCategory("general", 0, false),
                Category.PresetCategory("basic_needs", 1, false)
            ),
            selectedCategory = Category.PresetCategory("general", 0, false),
            currentPhrases = listOf(
                PhraseGridItem.Phrase("1", "Please"),
                PhraseGridItem.Phrase("2", "Thank you"),
                PhraseGridItem.Phrase("3", "Yes"),
                PhraseGridItem.Phrase("4", "No"),
                PhraseGridItem.Phrase("5", "Maybe"),
                PhraseGridItem.Phrase("6", "Please wait"),
                PhraseGridItem.AddPhrase
            ),
            isLoadingPhrases = false,
            onCategorySelected = {},
            onAddPhrase = {},
            onNavigateToKeyboard = {},
            onNavigateToSettings = {},
            onPhraseClick = { _, _ -> },
            isSpeaking = false,
            activeText = "Please"
        )
    }
}

@Preview(
    name = "Portrait",
    device = Devices.PIXEL_3,
    showBackground = true,
)
@Preview(
    name = "Landscape",
    device = "spec:width=800dp,height=400dp,dpi=240",
    showBackground = true,
)
@Composable
fun PresetsScreenPreview2() {
    VocableTheme {
        PresetsContent(
            categories = listOf(
                Category.PresetCategory(PresetCategories.USER_KEYPAD.id, PresetCategories.USER_KEYPAD.initialSortOrder, false),
                Category.PresetCategory(PresetCategories.BASIC_NEEDS.id, PresetCategories.BASIC_NEEDS.initialSortOrder, false)
            ),
            selectedCategory = Category.PresetCategory(PresetCategories.USER_KEYPAD.id, 0, false),
            currentPhrases = listOf(
                PhraseGridItem.Phrase("1", "0"),
                PhraseGridItem.Phrase("2", "1"),
                PhraseGridItem.Phrase("3", "2"),
                PhraseGridItem.Phrase("4", "3"),
                PhraseGridItem.Phrase("5", "4"),
                PhraseGridItem.Phrase("6", "5"),
                PhraseGridItem.Phrase("7", "6"),
                PhraseGridItem.Phrase("5", "7"),
                PhraseGridItem.Phrase("6", "8"),
                PhraseGridItem.Phrase("7", "9"),
                PhraseGridItem.Phrase("8", "Yes"),
                PhraseGridItem.Phrase("9", "No"),
                PhraseGridItem.AddPhrase
            ),
            isLoadingPhrases = false,
            onCategorySelected = {},
            onAddPhrase = {},
            onNavigateToKeyboard = {},
            onNavigateToSettings = {},
            onPhraseClick = { _, _ -> },
            isSpeaking = true,
            activeText = "0"
        )
    }
}

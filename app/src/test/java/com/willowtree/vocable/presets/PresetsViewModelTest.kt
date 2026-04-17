package com.willowtree.vocable.presets

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.FakePhrasesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.core.IdlingResourceContainerImpl
import com.willowtree.vocable.core.locale.LocalesWithText
import com.willowtree.vocable.data.room.CategoryDto
import com.willowtree.vocable.data.room.PhraseDto
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.domain.model.PhraseGridItem
import com.willowtree.vocable.domain.model.PresetCategories
import com.willowtree.vocable.ui.presets.PresetsIntent
import com.willowtree.vocable.ui.presets.PresetsViewModel
import com.willowtree.vocable.utils.FakeLocalizedResourceUtility
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PresetsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val fakeCategoriesUseCase = FakeCategoriesUseCase()
    private val fakePhrasesUseCase = FakePhrasesUseCase()
    private val prodIdlingResourceContainer = IdlingResourceContainerImpl()

    private fun createViewModel(): PresetsViewModel {
        return PresetsViewModel(
            fakeCategoriesUseCase,
            fakePhrasesUseCase,
            prodIdlingResourceContainer,
            FakeLocalizedResourceUtility()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `category list passed through and filters hidden`() = runTest(UnconfinedTestDispatcher()) {
        fakeCategoriesUseCase._categories.update {
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "category")),
                    hidden = false,
                    sortOrder = 0
                ),
                Category.StoredCategory(
                    categoryId = "2",
                    localizedName = LocalesWithText(mapOf("en_US" to "second category")),
                    hidden = true,
                    sortOrder = 0
                )
            )
        }

        val vm = createViewModel()

        assertEquals(
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "category")),
                    hidden = false,
                    sortOrder = 0
                )
            ),
            vm.state.value.categories
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `selected category set`() = runTest(UnconfinedTestDispatcher()) {
        fakeCategoriesUseCase._categories.update {
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "category")),
                    hidden = false,
                    sortOrder = 0
                ),
                Category.StoredCategory(
                    categoryId = "2",
                    localizedName = LocalesWithText(mapOf("en_US" to "second category")),
                    hidden = false,
                    sortOrder = 0
                )
            )
        }
        val vm = createViewModel()
        vm.onIntent(PresetsIntent.OnCategorySelected("1"))

        vm.state.test {
            assertEquals(
                Category.StoredCategory(
                    categoryId = "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "category")),
                    hidden = false,
                    sortOrder = 0
                ),
                awaitItem().selectedCategory
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `selected category is hidden and next immediate category is shown`() =
        runTest(UnconfinedTestDispatcher()) {
            fakeCategoriesUseCase._categories.update {
                listOf(
                    Category.StoredCategory(
                        categoryId = "1",
                        localizedName = LocalesWithText(mapOf("en_US" to "category")),
                        hidden = true,
                        sortOrder = 0
                    ),
                    Category.StoredCategory(
                        categoryId = "2",
                        localizedName = LocalesWithText(mapOf("en_US" to "second category")),
                        hidden = false,
                        sortOrder = 1
                    )
                )
            }

            val vm = createViewModel()

            vm.onIntent(PresetsIntent.OnCategorySelected("1"))

            assertEquals(
                null,
                vm.state.value.selectedCategory
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `selected category (last in list) is hidden and first category is shown`() =
        runTest(UnconfinedTestDispatcher()) {
            fakeCategoriesUseCase._categories.update {
                listOf(
                    Category.StoredCategory(
                        categoryId = "1",
                        localizedName = LocalesWithText(mapOf("en_US" to "category")),
                        hidden = false,
                        sortOrder = 0
                    ),
                    Category.StoredCategory(
                        categoryId = "2",
                        localizedName = LocalesWithText(mapOf("en_US" to "second category")),
                        hidden = false,
                        sortOrder = 1
                    ),
                    Category.StoredCategory(
                        categoryId = "3",
                        localizedName = LocalesWithText(mapOf("en_US" to "third category")),
                        hidden = true,
                        sortOrder = 2
                    )
                )
            }

            val vm = createViewModel()

            vm.onIntent(PresetsIntent.OnCategorySelected("3"))

            assertEquals(
                null,
                vm.state.value.selectedCategory
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `selected category is hidden and next non-hidden category is shown`() =
        runTest(UnconfinedTestDispatcher()) {
            fakeCategoriesUseCase._categories.update {
                listOf(
                    Category.StoredCategory(
                        categoryId = "1",
                        localizedName = LocalesWithText(mapOf("en_US" to "category")),
                        hidden = true,
                        sortOrder = 0
                    ),
                    Category.StoredCategory(
                        categoryId = "2",
                        localizedName = LocalesWithText(mapOf("en_US" to "second category")),
                        hidden = false,
                        sortOrder = 1
                    ),
                    Category.StoredCategory(
                        categoryId = "3",
                        localizedName = LocalesWithText(mapOf("en_US" to "third category")),
                        hidden = true,
                        sortOrder = 2
                    )
                )
            }

            val vm = createViewModel()

            vm.onIntent(PresetsIntent.OnCategorySelected("3"))

            assertEquals(
                null,
                vm.state.value.selectedCategory
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `current phrases updated when category ID changed`() = runTest(UnconfinedTestDispatcher()) {
        fakePhrasesUseCase._allCategories.update {
            listOf(
                CategoryDto(
                    categoryId = "1",
                    creationDate = 0L,
                    localizedName = LocalesWithText(mapOf("en_US" to "category")),
                    hidden = false,
                    sortOrder = 0
                ),
                CategoryDto(
                    categoryId = "2",
                    creationDate = 0L,
                    localizedName = LocalesWithText(mapOf("en_US" to "second category")),
                    hidden = false,
                    sortOrder = 0
                )
            )
        }
        fakePhrasesUseCase._categoriesToPhrases = mapOf(
            "1" to listOf(
                PhraseDto(
                    phraseId = "1",
                    parentCategoryId = "1",
                    creationDate = 0L,
                    lastSpokenDate = null,
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Hello")),
                    sortOrder = 0
                )
            ),
            "2" to listOf(
                PhraseDto(
                    phraseId = "2",
                    parentCategoryId = "2",
                    creationDate = 0L,
                    lastSpokenDate = null,
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Goodbye")),
                    sortOrder = 0
                )
            )
        )
        fakeCategoriesUseCase._categories.update {
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "category")),
                    hidden = false,
                    sortOrder = 0
                ),
                Category.StoredCategory(
                    categoryId = "2",
                    localizedName = LocalesWithText(mapOf("en_US" to "second category")),
                    hidden = false,
                    sortOrder = 1
                )
            )
        }
        val vm = createViewModel()
        vm.onIntent(PresetsIntent.OnCategorySelected("2"))

        assertEquals(
            listOf(
                PhraseGridItem.Phrase(
                    phraseId = "2",
                    text = "Goodbye",
                ),
                PhraseGridItem.AddPhrase
            ),
            vm.state.value.currentPhrases
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `non recents are sorted by sort order`() = runTest(UnconfinedTestDispatcher()) {
        fakePhrasesUseCase._allCategories.update {
            listOf(
                CategoryDto(
                    categoryId = "2",
                    creationDate = 0L,
                    localizedName = LocalesWithText(mapOf("en_US" to "category")),
                    hidden = false,
                    sortOrder = 0
                )
            )
        }
        fakePhrasesUseCase._categoriesToPhrases = mapOf(
            "2" to listOf(
                PhraseDto(
                    phraseId = "1",
                    parentCategoryId = "2",
                    creationDate = 0L,
                    lastSpokenDate = null,
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Hello")),
                    sortOrder = 1
                ),
                PhraseDto(
                    phraseId = "2",
                    parentCategoryId = "2",
                    creationDate = 0L,
                    lastSpokenDate = null,
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Goodbye")),
                    sortOrder = 0
                )
            )
        )
        fakeCategoriesUseCase._categories.update {
            listOf(
                Category.StoredCategory(
                    categoryId = "2",
                    localizedName = LocalesWithText(mapOf("en_US" to "category")),
                    hidden = false,
                    sortOrder = 0
                )
            )
        }
        val vm = createViewModel()
        vm.onIntent(PresetsIntent.OnCategorySelected("2"))
        assertEquals(
            listOf(
                PhraseGridItem.Phrase(
                    phraseId = "2",
                    text = "Goodbye",
                ),
                PhraseGridItem.Phrase(
                    phraseId = "1",
                    text = "Hello"
                ),
                PhraseGridItem.AddPhrase
            ),
            vm.state.value.currentPhrases
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `recents are not sorted by sort order`() = runTest(UnconfinedTestDispatcher()) {
        fakePhrasesUseCase._allCategories.update {
            listOf(
                CategoryDto(
                    categoryId = PresetCategories.RECENTS.id,
                    creationDate = 0L,
                    localizedName = LocalesWithText(mapOf("en_US" to "category")),
                    hidden = false,
                    sortOrder = 0
                )
            )
        }
        fakePhrasesUseCase._categoriesToPhrases = mapOf(
            PresetCategories.RECENTS.id to listOf(
                PhraseDto(
                    phraseId = "1",
                    parentCategoryId = PresetCategories.RECENTS.id,
                    creationDate = 0L,
                    lastSpokenDate = null,
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Hello")),
                    sortOrder = 1
                ),
                PhraseDto(
                    phraseId = "2",
                    parentCategoryId = PresetCategories.RECENTS.id,
                    creationDate = 0L,
                    lastSpokenDate = null,
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Goodbye")),
                    sortOrder = 0
                )
            )
        )
        fakeCategoriesUseCase._categories.update {
            listOf(
                Category.Recents(
                    hidden = false,
                    sortOrder = 0
                )
            )
        }
        val vm = createViewModel()
        vm.onIntent(PresetsIntent.OnCategorySelected(PresetCategories.RECENTS.id))
        assertEquals(
            listOf(
                PhraseGridItem.Phrase(
                    phraseId = "1",
                    text = "Hello",
                ),
                PhraseGridItem.Phrase(
                    phraseId = "2",
                    text = "Goodbye",
                )
            ),
            vm.state.value.currentPhrases
        )
    }
}

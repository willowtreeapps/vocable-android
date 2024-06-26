package com.willowtree.vocable.presets

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.FakePhrasesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.getOrAwaitValue
import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.utils.FakeLocalizedResourceUtility
import com.willowtree.vocable.utils.IdlingResourceContainerImpl
import com.willowtree.vocable.utils.locale.LocalesWithText
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

    @Test
    fun `category list passed through and filters hidden`() {
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
            vm.categoryList.getOrAwaitValue()
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
        vm.onCategorySelected("1")

        vm.selectedCategory.test {
            assertEquals(
                Category.StoredCategory(
                    categoryId = "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "category")),
                    hidden = false,
                    sortOrder = 0
                ),
                awaitItem()
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

            vm.onCategorySelected("1")

            vm.selectedCategory.test {
                assertEquals(
                    Category.StoredCategory(
                        categoryId = "2",
                        localizedName = LocalesWithText(mapOf("en_US" to "second category")),
                        hidden = false,
                        sortOrder = 1
                    ),
                    awaitItem()
                )
            }
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

            vm.onCategorySelected("3")

            vm.selectedCategory.test {
                assertEquals(
                    Category.StoredCategory(
                        categoryId = "1",
                        localizedName = LocalesWithText(mapOf("en_US" to "category")),
                        hidden = false,
                        sortOrder = 0
                    ),
                    awaitItem()
                )
            }
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

            vm.onCategorySelected("3")

            vm.selectedCategory.test {
                assertEquals(
                    Category.StoredCategory(
                        categoryId = "2",
                        localizedName = LocalesWithText(mapOf("en_US" to "second category")),
                        hidden = false,
                        sortOrder = 1
                    ),
                    awaitItem()
                )
            }
        }

    @Test
    fun `current phrases updated when category ID changed`() {
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
        val vm = createViewModel()
        vm.onCategorySelected("2")

        assertEquals(
            listOf(
                PhraseGridItem.Phrase(
                    phraseId = "2",
                    text = "Goodbye",
                ),
                PhraseGridItem.AddPhrase
            ),
            vm.currentPhrases.getOrAwaitValue()
        )
    }

    @Test
    fun `non recents are sorted by sort order`() {
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
        val vm = createViewModel()
        vm.onCategorySelected("2")
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
            vm.currentPhrases.getOrAwaitValue()
        )
    }

    @Test
    fun `recents are not sorted by sort order`() {
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
        val vm = createViewModel()
        vm.onCategorySelected(PresetCategories.RECENTS.id)
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
            vm.currentPhrases.getOrAwaitValue()
        )
    }

}
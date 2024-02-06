package com.willowtree.vocable.presets

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.FakePhrasesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.getOrAwaitValue
import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private fun createViewModel(): PresetsViewModel {
        return PresetsViewModel(
            fakeCategoriesUseCase,
            fakePhrasesUseCase,
        )
    }

    @Test
    fun `category list passed through`() {
        fakeCategoriesUseCase._categories.update {
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "category")),
                    hidden = false,
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

        //TODO: PK - Turbine may make this less painful, punting for now
        var category: Category? = null
        val job = launch {
            vm.selectedCategory.collect {
                category = it
            }
        }
        job.cancel()

        assertEquals(
            Category.StoredCategory(
                categoryId = "1",
                localizedName = LocalesWithText(mapOf("en_US" to "category")),
                hidden = false,
                sortOrder = 0
            ),
            category
        )
    }

    @Test
    fun `current phrases updated when category ID changed`() {
        fakePhrasesUseCase._allCategories.update {
            listOf(
                CategoryDto(
                    categoryId = "1",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = LocalesWithText(mapOf("en_US" to "category")),
                    hidden = false,
                    sortOrder = 0
                ),
                CategoryDto(
                    categoryId = "2",
                    creationDate = 0L,
                    resourceId = null,
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
                CustomPhrase(
                    phraseId = "2",
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Goodbye")),
                    sortOrder = 0,
                    lastSpokenDate = null,
                    parentCategoryId = "2",
                ),
                null
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
                    resourceId = null,
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
                CustomPhrase(
                    phraseId = "2",
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Goodbye")),
                    sortOrder = 0,
                    lastSpokenDate = null,
                    parentCategoryId = "2",
                ),
                CustomPhrase(
                    phraseId = "1",
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Hello")),
                    sortOrder = 1,
                    lastSpokenDate = null,
                    parentCategoryId = "2",
                ),
                null
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
                    resourceId = null,
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
                CustomPhrase(
                    phraseId = "1",
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Hello")),
                    sortOrder = 1,
                    lastSpokenDate = null,
                    parentCategoryId = PresetCategories.RECENTS.id,
                ),
                CustomPhrase(
                    phraseId = "2",
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Goodbye")),
                    sortOrder = 0,
                    lastSpokenDate = null,
                    parentCategoryId = PresetCategories.RECENTS.id,
                )
            ),
            vm.currentPhrases.getOrAwaitValue()
        )
    }

}
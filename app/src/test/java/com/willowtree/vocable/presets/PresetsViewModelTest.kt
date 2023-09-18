package com.willowtree.vocable.presets

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.CategoriesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.getOrAwaitValue
import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.Phrase
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

    private val fakePresetsRepository = FakePresetsRepository()

    private fun createViewModel(): PresetsViewModel {
        return PresetsViewModel(
            fakePresetsRepository,
            CategoriesUseCase(fakePresetsRepository)
        )
    }

    @Test
    fun `category list passed through`() {
        fakePresetsRepository._allCategories.update {
            listOf(
                CategoryDto(
                    categoryId = "1",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = mapOf("en_US" to "category"),
                    hidden = false,
                    sortOrder = 0
                )
            )
        }

        val vm = createViewModel()

        assertEquals(
            listOf(
                Category(
                    categoryId = "1",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = mapOf("en_US" to "category"),
                    hidden = false,
                    sortOrder = 0
                )
            ),
            vm.categoryList.getOrAwaitValue()
        )
    }

    @Test
    fun `selected category set`() = runTest(UnconfinedTestDispatcher()) {
        fakePresetsRepository._allCategories.update {
            listOf(
                CategoryDto(
                    categoryId = "1",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = mapOf("en_US" to "category"),
                    hidden = false,
                    sortOrder = 0
                ),
                CategoryDto(
                    categoryId = "2",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = mapOf("en_US" to "second category"),
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
            Category(
                categoryId = "1",
                creationDate = 0L,
                resourceId = null,
                localizedName = mapOf("en_US" to "category"),
                hidden = false,
                sortOrder = 0
            ),
            category
        )
    }

    @Test
    fun `current phrases updated when category ID changed`() {
        fakePresetsRepository._allCategories.update {
            listOf(
                CategoryDto(
                    categoryId = "1",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = mapOf("en_US" to "category"),
                    hidden = false,
                    sortOrder = 0
                ),
                CategoryDto(
                    categoryId = "2",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = mapOf("en_US" to "second category"),
                    hidden = false,
                    sortOrder = 0
                )
            )
        }
        fakePresetsRepository._categoriesToPhrases = mapOf(
            "1" to listOf(
                Phrase(
                    phraseId = 1L,
                    parentCategoryId = "1",
                    creationDate = 0L,
                    lastSpokenDate = 0L,
                    localizedUtterance = mapOf("en_US" to "Hello"),
                    sortOrder = 0
                )
            ),
            "2" to listOf(
                Phrase(
                    phraseId = 2L,
                    parentCategoryId = "2",
                    creationDate = 0L,
                    lastSpokenDate = 0L,
                    localizedUtterance = mapOf("en_US" to "Goodbye"),
                    sortOrder = 0
                )
            )
        )
        val vm = createViewModel()
        vm.onCategorySelected("2")

        assertEquals(
            listOf(
                Phrase(
                    phraseId = 2L,
                    parentCategoryId = "2",
                    creationDate = 0L,
                    lastSpokenDate = 0L,
                    localizedUtterance = mapOf("en_US" to "Goodbye"),
                    sortOrder = 0
                ),
                null
            ),
            vm.currentPhrases.getOrAwaitValue()
        )
    }

}
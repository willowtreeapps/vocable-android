package com.willowtree.vocable.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.getOrAwaitValue
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.FakeLegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditCategoryMenuViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val categoriesUseCase = FakeCategoriesUseCase()

    private fun createViewModel(): EditCategoryMenuViewModel {
        return EditCategoryMenuViewModel(
            FakeLegacyCategoriesAndPhrasesRepository(),
            categoriesUseCase
        )
    }

    @Test
    fun `last category remaining true`() {
        categoriesUseCase._categories.update {
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
        vm.updateCategoryById("1")

        assertTrue(
            vm.lastCategoryRemaining.getOrAwaitValue()
        )
    }

    @Test
    fun `update hidden status updates`() = runTest {
        categoriesUseCase._categories.update {
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
        vm.updateCategoryById("1")
        vm.updateCategoryShown(false)

        assertTrue(
            categoriesUseCase.getCategoryById("1").hidden
        )
    }

    @Test
    fun `delete category`() = runTest {
        categoriesUseCase._categories.update {
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
        vm.updateCategoryById("1")
        vm.deleteCategory()

        assertTrue(
            categoriesUseCase.getCategoryById("1") == null
        )
    }

}
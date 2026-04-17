package com.willowtree.vocable.settings

import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.core.locale.LocalesWithText
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.ui.editcategorymenu.EditCategoryMenuIntent
import com.willowtree.vocable.ui.editcategorymenu.EditCategoryMenuViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditCategoryMenuViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categoriesUseCase = FakeCategoriesUseCase()

    private fun createViewModel(): EditCategoryMenuViewModel {
        return EditCategoryMenuViewModel(categoriesUseCase)
    }

    @Test
    fun `last category remaining true`() = runTest(UnconfinedTestDispatcher()) {
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
        vm.loadCategory("1")

        assertTrue(vm.uiState.value.isLastCategory)
    }

    @Test
    fun `update hidden status updates`() = runTest(UnconfinedTestDispatcher()) {
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
        vm.loadCategory("1")
        vm.onIntent(EditCategoryMenuIntent.SetCategoryShown(false))

        assertTrue(categoriesUseCase.getCategoryById("1").hidden)
        assertTrue(vm.uiState.value.category?.hidden == true)
    }
}

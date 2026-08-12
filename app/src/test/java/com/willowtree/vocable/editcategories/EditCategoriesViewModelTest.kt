package com.willowtree.vocable.editcategories

import app.cash.turbine.test
import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.ui.editcategories.EditCategoriesEvent
import com.willowtree.vocable.ui.editcategories.EditCategoriesIntent
import com.willowtree.vocable.ui.editcategories.EditCategoriesViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditCategoriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        categoriesUseCase: FakeCategoriesUseCase = FakeCategoriesUseCase()
    ): EditCategoriesViewModel = EditCategoriesViewModel(categoriesUseCase)

    @Test
    fun `Back emits navigate back event`() = runTest {
        val viewModel = createViewModel()

        viewModel.event.test {
            viewModel.onIntent(EditCategoriesIntent.Back)
            assertEquals(EditCategoriesEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `AddCategory emits navigate to add category event`() = runTest {
        val viewModel = createViewModel()

        viewModel.event.test {
            viewModel.onIntent(EditCategoriesIntent.AddCategory)
            assertEquals(EditCategoriesEvent.NavigateToAddCategory, awaitItem())
        }
    }

    @Test
    fun `RequestResetCategories opens the reset dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(EditCategoriesIntent.RequestResetCategories)

        assertTrue(viewModel.uiState.value.isResetDialogOpen)
    }

    @Test
    fun `DismissResetDialog after RequestResetCategories makes no changes`() = runTest {
        val categoriesUseCase = FakeCategoriesUseCase()
        val initialSize = categoriesUseCase._categories.value.size
        val viewModel = createViewModel(categoriesUseCase = categoriesUseCase)
        viewModel.onIntent(EditCategoriesIntent.RequestResetCategories)

        viewModel.onIntent(EditCategoriesIntent.DismissResetDialog)

        assertFalse(viewModel.uiState.value.isResetDialogOpen)
        assertEquals(initialSize, categoriesUseCase._categories.value.size)
    }

    @Test
    fun `ConfirmResetDialog resets categories and closes the dialog`() = runTest {
        val categoriesUseCase = FakeCategoriesUseCase()
        val viewModel = createViewModel(categoriesUseCase = categoriesUseCase)
        viewModel.onIntent(EditCategoriesIntent.RequestResetCategories)

        viewModel.onIntent(EditCategoriesIntent.ConfirmResetDialog)

        assertFalse(viewModel.uiState.value.isResetDialogOpen)
        assertEquals(1, categoriesUseCase._categories.value.size)
        assertEquals(true, categoriesUseCase._categories.value.first() is Category.StoredCategory)
    }
}

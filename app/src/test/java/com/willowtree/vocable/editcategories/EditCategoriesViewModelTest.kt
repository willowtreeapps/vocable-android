package com.willowtree.vocable.editcategories

import app.cash.turbine.test
import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.ui.editcategories.EditCategoriesEvent
import com.willowtree.vocable.ui.editcategories.EditCategoriesIntent
import com.willowtree.vocable.ui.editcategories.EditCategoriesViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}

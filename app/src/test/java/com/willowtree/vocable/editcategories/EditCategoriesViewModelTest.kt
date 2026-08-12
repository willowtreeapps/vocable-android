package com.willowtree.vocable.editcategories

import app.cash.turbine.test
import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.FakePhrasesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.ui.editcategories.EditCategoriesEvent
import com.willowtree.vocable.ui.editcategories.EditCategoriesIntent
import com.willowtree.vocable.ui.editcategories.EditCategoriesViewModel
import com.willowtree.vocable.ui.editcategories.ResetTarget
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class EditCategoriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        categoriesUseCase: FakeCategoriesUseCase = FakeCategoriesUseCase(),
        phrasesUseCase: FakePhrasesUseCase = FakePhrasesUseCase()
    ): EditCategoriesViewModel = EditCategoriesViewModel(categoriesUseCase, phrasesUseCase)

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
    fun `RequestResetCategories opens the categories reset dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(EditCategoriesIntent.RequestResetCategories)

        assertEquals(ResetTarget.CATEGORIES, viewModel.uiState.value.resetTarget)
    }

    @Test
    fun `RequestResetPhrases opens the phrases reset dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(EditCategoriesIntent.RequestResetPhrases)

        assertEquals(ResetTarget.PHRASES, viewModel.uiState.value.resetTarget)
    }

    @Test
    fun `DismissResetDialog after RequestResetCategories makes no changes`() = runTest {
        val categoriesUseCase = FakeCategoriesUseCase()
        val initialSize = categoriesUseCase._categories.value.size
        val viewModel = createViewModel(categoriesUseCase = categoriesUseCase)
        viewModel.onIntent(EditCategoriesIntent.RequestResetCategories)

        viewModel.onIntent(EditCategoriesIntent.DismissResetDialog)

        assertNull(viewModel.uiState.value.resetTarget)
        assertEquals(initialSize, categoriesUseCase._categories.value.size)
    }

    @Test
    fun `ConfirmResetDialog for categories resets categories only`() = runTest {
        val categoriesUseCase = FakeCategoriesUseCase()
        val viewModel = createViewModel(categoriesUseCase = categoriesUseCase)
        viewModel.onIntent(EditCategoriesIntent.RequestResetCategories)

        viewModel.onIntent(EditCategoriesIntent.ConfirmResetDialog)

        assertNull(viewModel.uiState.value.resetTarget)
        assertEquals(1, categoriesUseCase._categories.value.size)
        assertEquals(true, categoriesUseCase._categories.value.first() is Category.StoredCategory)
    }

    @Test
    fun `ConfirmResetDialog for phrases resets phrases only`() = runTest {
        val phrasesUseCase = FakePhrasesUseCase()
        phrasesUseCase._categoriesToPhrases = emptyMap()
        val viewModel = createViewModel(phrasesUseCase = phrasesUseCase)
        viewModel.onIntent(EditCategoriesIntent.RequestResetPhrases)

        viewModel.onIntent(EditCategoriesIntent.ConfirmResetDialog)

        assertNull(viewModel.uiState.value.resetTarget)
        assertEquals(1, phrasesUseCase.getPhrasesForCategory("1").size)
    }
}

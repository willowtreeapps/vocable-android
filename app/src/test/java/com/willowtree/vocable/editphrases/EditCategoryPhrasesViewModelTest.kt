package com.willowtree.vocable.editphrases

import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.FakePhrasesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.core.locale.LocalesWithText
import com.willowtree.vocable.data.room.PhraseDto
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.ui.editphrases.EditCategoryPhrasesIntent
import com.willowtree.vocable.ui.editphrases.EditCategoryPhrasesViewModel
import com.willowtree.vocable.utils.FakeLocalizedResourceUtility
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditCategoryPhrasesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categoryId = "categoryId"

    private fun createViewModel(
        categoriesUseCase: FakeCategoriesUseCase = FakeCategoriesUseCase(),
        phrasesUseCase: FakePhrasesUseCase = FakePhrasesUseCase()
    ): EditCategoryPhrasesViewModel =
        EditCategoryPhrasesViewModel(categoriesUseCase, phrasesUseCase, FakeLocalizedResourceUtility())

    private fun phrasesUseCaseFor(categoryId: String): FakePhrasesUseCase {
        val phrasesUseCase = FakePhrasesUseCase()
        phrasesUseCase._categoriesToPhrases = mapOf(
            categoryId to listOf(
                PhraseDto(
                    phraseId = "custom-phrase",
                    parentCategoryId = categoryId,
                    creationDate = 0L,
                    lastSpokenDate = null,
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Custom phrase")),
                    sortOrder = 0
                )
            )
        )
        return phrasesUseCase
    }

    @Test
    fun `RequestReset opens the reset dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.loadCategory(categoryId)

        viewModel.onIntent(EditCategoryPhrasesIntent.RequestReset)

        assertTrue(viewModel.uiState.value.isResetDialogOpen)
    }

    @Test
    fun `DismissResetDialog after RequestReset makes no changes`() = runTest {
        val phrasesUseCase = phrasesUseCaseFor(categoryId)
        val viewModel = createViewModel(phrasesUseCase = phrasesUseCase)
        viewModel.loadCategory(categoryId)
        viewModel.onIntent(EditCategoryPhrasesIntent.RequestReset)

        viewModel.onIntent(EditCategoryPhrasesIntent.DismissResetDialog)

        assertFalse(viewModel.uiState.value.isResetDialogOpen)
        assertEquals(1, phrasesUseCase.getPhrasesForCategory(categoryId).size)
    }

    @Test
    fun `ConfirmResetDialog resets only this category's phrases and closes the dialog`() = runTest {
        val phrasesUseCase = phrasesUseCaseFor(categoryId)
        val viewModel = createViewModel(phrasesUseCase = phrasesUseCase)
        viewModel.loadCategory(categoryId)
        viewModel.onIntent(EditCategoryPhrasesIntent.RequestReset)

        viewModel.onIntent(EditCategoryPhrasesIntent.ConfirmResetDialog)

        assertFalse(viewModel.uiState.value.isResetDialogOpen)
        assertEquals(0, phrasesUseCase.getPhrasesForCategory(categoryId).size)
    }

    @Test
    fun `ConfirmResetDialog does not touch other categories' phrases`() = runTest {
        val otherCategoryId = "otherCategoryId"
        val phrasesUseCase = FakePhrasesUseCase()
        phrasesUseCase._categoriesToPhrases = mapOf(
            categoryId to listOf(
                PhraseDto("p1", categoryId, 0L, null, LocalesWithText(mapOf("en_US" to "A")), 0)
            ),
            otherCategoryId to listOf(
                PhraseDto("p2", otherCategoryId, 0L, null, LocalesWithText(mapOf("en_US" to "B")), 0)
            )
        )
        val categoriesUseCase = FakeCategoriesUseCase()
        categoriesUseCase._categories.value = listOf(
            Category.StoredCategory(categoryId, LocalesWithText(mapOf("en_US" to "General")), false, 0),
            Category.StoredCategory(otherCategoryId, LocalesWithText(mapOf("en_US" to "Other")), false, 1)
        )
        val viewModel = createViewModel(categoriesUseCase = categoriesUseCase, phrasesUseCase = phrasesUseCase)
        viewModel.loadCategory(categoryId)
        viewModel.onIntent(EditCategoryPhrasesIntent.RequestReset)

        viewModel.onIntent(EditCategoryPhrasesIntent.ConfirmResetDialog)

        assertEquals(0, phrasesUseCase.getPhrasesForCategory(categoryId).size)
        assertEquals(1, phrasesUseCase.getPhrasesForCategory(otherCategoryId).size)
    }
}

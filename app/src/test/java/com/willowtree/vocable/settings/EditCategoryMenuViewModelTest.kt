package com.willowtree.vocable.settings

import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.FakePhrasesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.core.locale.LocalesWithText
import com.willowtree.vocable.data.room.PhraseDto
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.ui.editcategorymenu.EditCategoryMenuIntent
import com.willowtree.vocable.ui.editcategorymenu.EditCategoryMenuViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditCategoryMenuViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categoriesUseCase = FakeCategoriesUseCase()
    private val phrasesUseCase = FakePhrasesUseCase()

    private fun createViewModel(): EditCategoryMenuViewModel {
        return EditCategoryMenuViewModel(categoriesUseCase, phrasesUseCase)
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

    @Test
    fun `RequestResetCategory opens the reset dialog`() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()
        vm.loadCategory("categoryId")

        vm.onIntent(EditCategoryMenuIntent.RequestResetCategory)

        assertTrue(vm.uiState.value.isResetDialogOpen)
    }

    @Test
    fun `DismissResetDialog after RequestResetCategory makes no changes`() = runTest(UnconfinedTestDispatcher()) {
        phrasesUseCase._categoriesToPhrases = mapOf(
            "categoryId" to listOf(
                PhraseDto(
                    phraseId = "custom-phrase",
                    parentCategoryId = "categoryId",
                    creationDate = 0L,
                    lastSpokenDate = null,
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Custom phrase")),
                    sortOrder = 0
                )
            )
        )
        val vm = createViewModel()
        vm.loadCategory("categoryId")
        vm.onIntent(EditCategoryMenuIntent.RequestResetCategory)

        vm.onIntent(EditCategoryMenuIntent.DismissResetDialog)

        assertFalse(vm.uiState.value.isResetDialogOpen)
        assertEquals(1, phrasesUseCase.getPhrasesForCategory("categoryId").size)
    }

    @Test
    fun `ConfirmResetDialog resets this category's phrases and closes the dialog`() = runTest(UnconfinedTestDispatcher()) {
        phrasesUseCase._categoriesToPhrases = mapOf(
            "categoryId" to listOf(
                PhraseDto(
                    phraseId = "custom-phrase",
                    parentCategoryId = "categoryId",
                    creationDate = 0L,
                    lastSpokenDate = null,
                    localizedUtterance = LocalesWithText(mapOf("en_US" to "Custom phrase")),
                    sortOrder = 0
                )
            )
        )
        val vm = createViewModel()
        vm.loadCategory("categoryId")
        vm.onIntent(EditCategoryMenuIntent.RequestResetCategory)

        vm.onIntent(EditCategoryMenuIntent.ConfirmResetDialog)

        assertFalse(vm.uiState.value.isResetDialogOpen)
        assertEquals(0, phrasesUseCase.getPhrasesForCategory("categoryId").size)
    }
}

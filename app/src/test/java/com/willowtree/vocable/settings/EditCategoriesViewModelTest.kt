package com.willowtree.vocable.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.CategoriesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.presets.FakePresetsRepository
import com.willowtree.vocable.presets.createStoredCategory
import com.willowtree.vocable.room.createCategoryDto
import com.willowtree.vocable.utils.FakeLocalizedResourceUtility
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EditCategoriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val fakePresetsRepository = FakePresetsRepository()

    private fun createViewModel(): EditCategoriesViewModel {
        return EditCategoriesViewModel(
            fakePresetsRepository,
            CategoriesUseCase(fakePresetsRepository),
            FakeLocalizedResourceUtility()
        )
    }

    @Test
    fun `categories are populated`() {
        fakePresetsRepository._allCategories.update {
            listOf(
                createCategoryDto(categoryId = "1")
            )
        }
        val vm = createViewModel()
        vm.refreshCategories()

        assertEquals(
            listOf(
                createStoredCategory(categoryId = "1")
            ),
            vm.orderCategoryList.value
        )

        assertEquals(
            listOf(
                createStoredCategory(categoryId = "1")
            ),
            vm.addRemoveCategoryList.value
        )
    }

}
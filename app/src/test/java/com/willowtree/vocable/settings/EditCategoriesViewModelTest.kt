package com.willowtree.vocable.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.CategoriesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.PhrasesUseCase
import com.willowtree.vocable.presets.FakePresetsRepository
import com.willowtree.vocable.presets.createStoredCategory
import com.willowtree.vocable.room.createCategoryDto
import com.willowtree.vocable.utils.ConstantUUIDProvider
import com.willowtree.vocable.utils.FakeDateProvider
import com.willowtree.vocable.utils.FakeLocaleProvider
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
            PhrasesUseCase(fakePresetsRepository, FakeDateProvider()),
            CategoriesUseCase(fakePresetsRepository, ConstantUUIDProvider(), FakeDateProvider(), FakeLocaleProvider()),
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

    @Test
    fun `move category up`() {
        fakePresetsRepository._allCategories.update {
            listOf(
                createCategoryDto(
                    categoryId = "1",
                    sortOrder = 0
                ),
                createCategoryDto(
                    categoryId = "2",
                    sortOrder = 1
                )
            )
        }
        val vm = createViewModel()
        vm.refreshCategories()
        vm.moveCategoryUp("2")

        assertEquals(
            listOf(
                createStoredCategory(
                    categoryId = "2",
                    sortOrder = 0
                ),
                createStoredCategory(
                    categoryId = "1",
                    sortOrder = 1
                )
            ),
            vm.orderCategoryList.value
        )
        assertEquals(
            listOf(
                createCategoryDto(
                    categoryId = "1",
                    sortOrder = 1
                ),
                createCategoryDto(
                    categoryId = "2",
                    sortOrder = 0
                )
            ),
            fakePresetsRepository._allCategories.value
        )
    }

    @Test
    fun `move category down`() {
        fakePresetsRepository._allCategories.update {
            listOf(
                createCategoryDto(
                    categoryId = "1",
                    sortOrder = 0
                ),
                createCategoryDto(
                    categoryId = "2",
                    sortOrder = 1
                )
            )
        }
        val vm = createViewModel()
        vm.refreshCategories()
        vm.moveCategoryDown("1")

        assertEquals(
            listOf(
                createStoredCategory(
                    categoryId = "2",
                    sortOrder = 0
                ),
                createStoredCategory(
                    categoryId = "1",
                    sortOrder = 1
                )
            ),
            vm.orderCategoryList.value
        )
        assertEquals(
            listOf(
                createCategoryDto(
                    categoryId = "1",
                    sortOrder = 1
                ),
                createCategoryDto(
                    categoryId = "2",
                    sortOrder = 0
                )
            ),
            fakePresetsRepository._allCategories.value
        )
    }

}
package com.willowtree.vocable.settings

import app.cash.turbine.test
import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.presets.createStoredCategory
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EditCategoriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categoriesUseCase = FakeCategoriesUseCase()

    private fun createViewModel(): EditCategoriesViewModel {
        return EditCategoriesViewModel(
            categoriesUseCase
        )
    }

    @Test
    fun `categories are populated`() = runTest {
        categoriesUseCase._categories.update {
            listOf(
                createStoredCategory(categoryId = "1")
            )
        }
        val vm = createViewModel()
        vm.refreshCategories()

        vm.categoryList.test {
            assertEquals(
                listOf(
                    createStoredCategory(categoryId = "1")
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `move category up`() = runTest {
        categoriesUseCase._categories.update {
            listOf(
                createStoredCategory(
                    categoryId = "1",
                    sortOrder = 0
                ),
                createStoredCategory(
                    categoryId = "2",
                    sortOrder = 1
                )
            )
        }
        val vm = createViewModel()
        vm.refreshCategories()
        vm.moveCategoryUp("2")

        vm.categoryList.test {
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
                awaitItem()
            )
        }
    }

    @Test
    fun `move category down`() = runTest {
        categoriesUseCase._categories.update {
            listOf(
                createStoredCategory(
                    categoryId = "1",
                    sortOrder = 0
                ),
                createStoredCategory(
                    categoryId = "2",
                    sortOrder = 1
                )
            )
        }
        val vm = createViewModel()
        vm.refreshCategories()
        vm.moveCategoryDown("1")

        vm.categoryList.test {
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
                awaitItem()
            )
        }
    }

}
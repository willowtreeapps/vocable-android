package com.willowtree.vocable.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.FakePhrasesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.presets.createStoredCategory
import com.willowtree.vocable.settings.editcategories.EditCategoriesPage
import com.willowtree.vocable.utils.FakeLocalizedResourceUtility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EditCategoriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val categoriesUseCase = FakeCategoriesUseCase()
    private val phrasesUseCase = FakePhrasesUseCase()

    private fun createViewModel(): EditCategoriesViewModel {
        return EditCategoriesViewModel(
            phrasesUseCase,
            categoriesUseCase,
            FakeLocalizedResourceUtility()
        )
    }

    @Test
    fun `categories are populated`() {
        categoriesUseCase._categories.update {
            listOf(
                createStoredCategory(categoryId = "1")
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
                createStoredCategory(
                    categoryId = "1",
                    sortOrder = 1
                ),
                createStoredCategory(
                    categoryId = "2",
                    sortOrder = 0
                )
            ),
            categoriesUseCase._categories.value
        )
    }

    @Test
    fun `move category down`() {
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
                createStoredCategory(
                    categoryId = "1",
                    sortOrder = 1
                ),
                createStoredCategory(
                    categoryId = "2",
                    sortOrder = 0
                )
            ),
            categoriesUseCase._categories.value
        )
    }

    @Test
    fun `category pages are populated`() = runTest {
        categoriesUseCase._categories.update {
            listOf(
                createStoredCategory(categoryId = "1"),
                createStoredCategory(categoryId = "2"),
                createStoredCategory(categoryId = "3"),
                createStoredCategory(categoryId = "4"),
                createStoredCategory(categoryId = "5"),
                createStoredCategory(categoryId = "6"),
                createStoredCategory(categoryId = "7"),
                createStoredCategory(categoryId = "8"),
                createStoredCategory(categoryId = "9"),
            )
        }
        val vm = createViewModel()
        vm.refreshCategories()

        assertEquals(
            listOf(
                EditCategoriesPage(
                    listOf(
                        createStoredCategory(categoryId = "1"),
                        createStoredCategory(categoryId = "2"),
                        createStoredCategory(categoryId = "3"),
                        createStoredCategory(categoryId = "4"),
                        createStoredCategory(categoryId = "5"),
                        createStoredCategory(categoryId = "6"),
                        createStoredCategory(categoryId = "7"),
                        createStoredCategory(categoryId = "8"),
                    )
                ),
                EditCategoriesPage(
                    listOf(
                        createStoredCategory(categoryId = "9"),
                    )
                )
            ),
            vm.categoryPages.first()
        )
    }

}
package com.willowtree.vocable.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.utils.FakeLocaleProvider
import com.willowtree.vocable.utils.FakeLocalizedResourceUtility
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddUpdateCategoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val categoriesUseCase = FakeCategoriesUseCase()

    private fun createViewModel(): AddUpdateCategoryViewModel {
        return AddUpdateCategoryViewModel(
            categoriesUseCase,
            FakeLocalizedResourceUtility(),
            FakeLocaleProvider()
        )
    }

    @Test
    fun `new category added before hidden categories`() {
        categoriesUseCase._categories.update {
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    resourceId = null,
                    localizedName = null,
                    hidden = false,
                    sortOrder = 0
                ),
                Category.StoredCategory(
                    categoryId = "2",
                    resourceId = null,
                    localizedName = null,
                    hidden = true,
                    sortOrder = 1
                )
            )
        }

        val vm = createViewModel()

        vm.addCategory("New Category")

        assertEquals(
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    resourceId = null,
                    localizedName = null,
                    hidden = false,
                    sortOrder = 0
                ),
                Category.StoredCategory(
                    categoryId = "2",
                    resourceId = null,
                    localizedName = null,
                    hidden = true,
                    sortOrder = 2
                ),
                Category.StoredCategory(
                    categoryId = "",
                    resourceId = null,
                    localizedName = LocalesWithText(mapOf("en_US" to "New Category")),
                    hidden = false,
                    sortOrder = 1
                )
            ),
            categoriesUseCase._categories.value
        )
    }

    @Test
    fun `category name updated`() = runTest {
        categoriesUseCase._categories.update {
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    resourceId = null,
                    localizedName = LocalesWithText(mapOf("en_US" to "Category")),
                    hidden = false,
                    sortOrder = 0
                )
            )
        }

        val vm = createViewModel()

        vm.updateCategory("1", "New Category")

        assertEquals(
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    resourceId = null,
                    localizedName = LocalesWithText(mapOf("en_US" to "New Category")),
                    hidden = false,
                    sortOrder = 0
                )
            ),
            categoriesUseCase.categories().first()
        )
    }

    @Test
    fun `category name updated does not wipe other locale`() = runTest {
        categoriesUseCase._categories.update {
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    resourceId = null,
                    localizedName = LocalesWithText(mapOf("en_US" to "Category", "es_US" to "Spanish")),
                    hidden = false,
                    sortOrder = 0
                )
            )
        }

        val vm = createViewModel()

        vm.updateCategory("1", "New Category")

        assertEquals(
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    resourceId = null,
                    localizedName = LocalesWithText(mapOf("en_US" to "New Category", "es_US" to "Spanish")),
                    hidden = false,
                    sortOrder = 0
                )
            ),
            categoriesUseCase.categories().first()
        )
    }

}
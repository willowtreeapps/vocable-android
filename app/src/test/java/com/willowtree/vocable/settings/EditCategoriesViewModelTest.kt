package com.willowtree.vocable.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.PhrasesUseCase
import com.willowtree.vocable.PhrasesUseCaseTest
import com.willowtree.vocable.presets.FakeLegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.presets.createStoredCategory
import com.willowtree.vocable.utils.FakeDateProvider
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

    private val fakePresetsRepository = FakeLegacyCategoriesAndPhrasesRepository()
    private val categoriesUseCase = FakeCategoriesUseCase()

    private fun createViewModel(): EditCategoriesViewModel {
        return EditCategoriesViewModel(
            PhrasesUseCase(
                legacyPhrasesRepository = fakePresetsRepository,
                storedPhrasesRepository = PhrasesUseCaseTest.StubStoredPhrasesRepository(),
                presetPhrasesRepository = PhrasesUseCaseTest.StubPresetPhrasesRepository(),
                dateProvider = FakeDateProvider(),
            ),
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

}
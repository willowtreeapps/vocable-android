package com.willowtree.vocable.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.CategoriesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.FakePresetsRepository
import com.willowtree.vocable.room.createCategoryDto
import com.willowtree.vocable.utils.ConstantUUIDProvider
import com.willowtree.vocable.utils.FakeDateProvider
import com.willowtree.vocable.utils.FakeLocaleProvider
import com.willowtree.vocable.utils.FakeLocalizedResourceUtility
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

    private val presetsRepository = FakePresetsRepository()
    private val categoriesUseCase = CategoriesUseCase(presetsRepository)
    private val uuidProvider = ConstantUUIDProvider()
    private val dateProvider = FakeDateProvider()

    private fun createViewModel(): AddUpdateCategoryViewModel {
        return AddUpdateCategoryViewModel(
            categoriesUseCase,
            FakeLocalizedResourceUtility(),
            uuidProvider,
            dateProvider,
            FakeLocaleProvider()
        )
    }

    @Test
    fun `category added`() = runTest {
        presetsRepository._allCategories.update {
            listOf(
                createCategoryDto(
                    categoryId = "1",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = null,
                    hidden = false,
                    sortOrder = 0
                )
            )
        }
        uuidProvider._uuid = "2"
        dateProvider._currentTimeMillis = 0L

        val vm = createViewModel()

        vm.addCategory(
            "New Category"
        )

        assertEquals(
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = null,
                    hidden = false,
                    sortOrder = 0
                ),
                Category.StoredCategory(
                    categoryId = "2",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = mapOf("en_US" to "New Category"),
                    hidden = false,
                    sortOrder = 1
                )
            ),
            categoriesUseCase.categories().first()
        )
    }

    @Test
    fun `new category added before hidden categories`() {
        presetsRepository._allCategories.update {
            listOf(
                createCategoryDto(
                    categoryId = "1",
                    hidden = false,
                    sortOrder = 0
                ),
                createCategoryDto(
                    categoryId = "2",
                    hidden = true,
                    sortOrder = 1
                )
            )
        }

        uuidProvider._uuid = "3"
        val vm = createViewModel()

        vm.addCategory("New Category")

        assertEquals(
            listOf(
                createCategoryDto(
                    categoryId = "1",
                    hidden = false,
                    sortOrder = 0
                ),
                createCategoryDto(
                    categoryId = "2",
                    hidden = true,
                    sortOrder = 2
                ),
                createCategoryDto(
                    categoryId = "3",
                    hidden = false,
                    sortOrder = 1,
                    localizedName = mapOf("en_US" to "New Category")
                )
            ),
            presetsRepository._allCategories.value
        )
    }

    @Test
    fun `category updated`() = runTest {
        presetsRepository._allCategories.update {
            listOf(
                createCategoryDto(
                    categoryId = "1",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = mapOf("en_US" to "Category"),
                    hidden = false,
                    sortOrder = 0
                )
            )
        }
        uuidProvider._uuid = "2"
        dateProvider._currentTimeMillis = 0L

        val vm = createViewModel()

        vm.updateCategory("1", "New Category")

        assertEquals(
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = mapOf("en_US" to "New Category"),
                    hidden = false,
                    sortOrder = 0
                )
            ),
            categoriesUseCase.categories().first()
        )
    }

}
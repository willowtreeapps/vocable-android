package com.willowtree.vocable.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.CategoriesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.FakePresetsRepository
import com.willowtree.vocable.room.createCategoryDto
import com.willowtree.vocable.utils.ConstantUUIDProvider
import com.willowtree.vocable.utils.FakeDateProvider
import com.willowtree.vocable.utils.FakeLocalizedResourceUtility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert
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
            dateProvider
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

        Assert.assertEquals(
            listOf(
                Category(
                    categoryId = "1",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = null,
                    hidden = false,
                    sortOrder = 0
                ),
                Category(
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

        Assert.assertEquals(
            listOf(
                Category(
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
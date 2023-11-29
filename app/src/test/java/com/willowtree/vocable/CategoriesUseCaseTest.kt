package com.willowtree.vocable

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.FakePresetCategoriesRepository
import com.willowtree.vocable.presets.FakePresetsRepository
import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.FakeStoredCategoriesRepository
import com.willowtree.vocable.room.createCategoryDto
import com.willowtree.vocable.utils.ConstantUUIDProvider
import com.willowtree.vocable.utils.FakeLocaleProvider
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoriesUseCaseTest {

    private val fakePresetsRepository = FakePresetsRepository()
    private val fakeStoredCategoriesRepository = FakeStoredCategoriesRepository()
    private val fakePresetCategoriesRepository = FakePresetCategoriesRepository()

    private fun createUseCase(): CategoriesUseCase {
        return CategoriesUseCase(
            fakePresetsRepository,
            ConstantUUIDProvider(),
            FakeLocaleProvider(),
            fakeStoredCategoriesRepository,
            fakePresetCategoriesRepository
        )
    }

    @Test
    fun `preset and stored categories returned`() = runTest {
        fakeStoredCategoriesRepository._allCategories.update {
            listOf(
                createCategoryDto(
                    "customCategory1",
                    localizedName = LocalesWithText(mapOf("en_US" to "Custom")),
                    resourceId = 1
                ),
                createCategoryDto(
                    "customCategory2",
                    localizedName = LocalesWithText(mapOf("en_US" to "Other")),
                    resourceId = 2
                )
            )
        }

        fakePresetCategoriesRepository._presetCategories = listOf(
            Category.PresetCategory(
                categoryId = "presetCategory",
                sortOrder = 3,
                hidden = false,
                resourceId = 0
            )
        )

        val useCase = createUseCase()

        assertEquals(
            listOf(
                Category.StoredCategory(
                    categoryId = "customCategory1",
                    resourceId = 1,
                    localizedName = LocalesWithText(mapOf("en_US" to "Custom")),
                    hidden = false,
                    sortOrder = 0
                ),
                Category.StoredCategory(
                    categoryId = "customCategory2",
                    resourceId = 2,
                    localizedName = LocalesWithText(mapOf("en_US" to "Other")),
                    hidden = false,
                    sortOrder = 0
                ),
                Category.PresetCategory(
                    categoryId = "presetCategory",
                    sortOrder = 3,
                    hidden = false,
                    resourceId = 0
                )
            ),
            useCase.categories().first()
        )
    }

    @Test
    fun `category added to stored repository`() = runTest {
        fakeStoredCategoriesRepository._allCategories.update { emptyList() }

        fakePresetCategoriesRepository._presetCategories = listOf(
            Category.PresetCategory(
                categoryId = "presetCategory",
                sortOrder = 3,
                hidden = false,
                resourceId = 0
            )
        )

        val useCase = createUseCase()

        useCase.addCategory("My Category", 0)

        assertEquals(
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    resourceId = null,
                    localizedName = LocalesWithText(mapOf("en_US" to "My Category")),
                    hidden = false,
                    sortOrder = 0
                ),
                Category.PresetCategory(
                    categoryId = "presetCategory",
                    sortOrder = 3,
                    hidden = false,
                    resourceId = 0
                ),
            ),
            useCase.categories().first()
        )
        assertEquals(
            listOf(
                CategoryDto(
                    categoryId = "1",
                    creationDate = 0L,
                    resourceId = null,
                    localizedName = LocalesWithText(mapOf("en_US" to "My Category")),
                    hidden = false,
                    sortOrder = 0
                )
            ),
            fakeStoredCategoriesRepository._allCategories.first()
        )
    }

}
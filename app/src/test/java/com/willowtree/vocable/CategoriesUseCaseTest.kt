package com.willowtree.vocable

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.FakePresetsRepository
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.room.createCategoryDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoriesUseCaseTest {

    private val fakePresetsRepository = FakePresetsRepository()

    private fun createUseCase(): CategoriesUseCase {
        return CategoriesUseCase(fakePresetsRepository)
    }

    @Test
    fun `preset categories transformed`() = runTest {
        fakePresetsRepository._allCategories.update {
            listOf(
                createCategoryDto(
                    PresetCategories.RECENTS.id,
                    localizedName = mapOf("en_US" to "Recents"),
                    resourceId = 1
                ),
                createCategoryDto(
                    "1",
                    localizedName = mapOf("en_US" to "Other"),
                    resourceId = 2
                )
            )
        }

        val useCase = createUseCase()

        assertEquals(
            listOf(
                Category.Recents(
                    hidden = false,
                    sortOrder = 0,
                    localizedName = mapOf("en_US" to "Recents"),
                    resourceId = 1
                ),
                Category.StoredCategory(
                    categoryId = "1",
                    creationDate = 0L,
                    resourceId = 2,
                    localizedName = mapOf("en_US" to "Other"),
                    hidden = false,
                    sortOrder = 0
                )
            ),
            useCase.categories().first()
        )
    }

}
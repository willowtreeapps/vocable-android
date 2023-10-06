package com.willowtree.vocable.presets

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PresetCategoriesRepositoryTest {
    private fun createRepository(): PresetCategoriesRepository {
        return PresetCategoriesRepository(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun preset_categories_returned() = runTest {
        val repository = createRepository()

        assertEquals(
            PresetCategories.values().filter { it != PresetCategories.MY_SAYINGS }.map {
                Category.PresetCategory(
                    it.id,
                    it.initialSortOrder,
                    false,
                    it.getNameId()
                )
            },
            repository.getPresetCategories()
        )
    }
}
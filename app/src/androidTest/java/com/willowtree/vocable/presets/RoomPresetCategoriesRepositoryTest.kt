package com.willowtree.vocable.presets

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.room.VocableDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomPresetCategoriesRepositoryTest {
    private fun createRepository(): RoomPresetCategoriesRepository {
        return RoomPresetCategoriesRepository(Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VocableDatabase::class.java
        ).build())
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
                )
            },
            repository.getPresetCategories().first()
        )
    }
}
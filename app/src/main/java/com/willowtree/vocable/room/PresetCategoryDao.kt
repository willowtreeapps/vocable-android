package com.willowtree.vocable.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PresetCategoryDao {
    @Insert
    suspend fun insertPresetCategory(presetCategoryDto: PresetCategoryDto)

    @Query("SELECT * FROM PresetCategory")
    suspend fun getPresetCategoryDtos(): List<PresetCategoryDto>
}
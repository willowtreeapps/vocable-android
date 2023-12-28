package com.willowtree.vocable.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PresetCategoryDao {
    @Insert
    suspend fun insertPresetCategory(presetCategoryDto: PresetCategoryDto)

    @Query("SELECT * FROM PresetCategory")
    suspend fun getPresetCategoryDtos(): List<PresetCategoryDto>

    @Update(entity = PresetCategoryDto::class)
    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>)

    @Query("SELECT * FROM PresetCategory WHERE category_id = :categoryId")
    suspend fun getPresetCategoryById(categoryId: String): PresetCategoryDto?
}
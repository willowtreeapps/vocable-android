package com.willowtree.vocable.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetCategoryDao {
    @Insert
    suspend fun insertPresetCategory(presetCategoryDto: PresetCategoryDto)

    @Update(entity = PresetCategoryDto::class)
    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>)

    @Update(entity = PresetCategoryDto::class)
    suspend fun updateCategoryHidden(categoryHidden: PresetCategoryHidden)

    @Query("SELECT * FROM PresetCategory WHERE category_id = :categoryId")
    suspend fun getPresetCategoryById(categoryId: String): PresetCategoryDto?

    @Query("SELECT * FROM PresetCategory WHERE category_id != 'preset_user_favorites'")
    fun getAllPresetCategoriesFlow(): Flow<List<PresetCategoryDto>>

    @Update(entity = PresetCategoryDto::class)
    suspend fun updateCategoryDeleted(categoryDeleted: PresetCategoryDeleted)
}
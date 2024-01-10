package com.willowtree.vocable.presets

import com.willowtree.vocable.room.CategorySortOrder
import kotlinx.coroutines.flow.Flow

interface PresetCategoriesRepository {
    fun getPresetCategories(): Flow<List<Category.PresetCategory>>
    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>)
    suspend fun getCategoryById(categoryId: String): Category.PresetCategory?
    suspend fun hidePresetCategory(categoryId: String)
}
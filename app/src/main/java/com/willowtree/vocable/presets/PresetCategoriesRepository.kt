package com.willowtree.vocable.presets

import com.willowtree.vocable.room.CategorySortOrder

interface PresetCategoriesRepository {
    suspend fun getPresetCategories(): List<Category.PresetCategory>
    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>)
    suspend fun getCategoryById(categoryId: String): Category.PresetCategory?
    suspend fun hidePresetCategory(categoryId: String)
}
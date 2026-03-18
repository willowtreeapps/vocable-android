package com.willowtree.vocable.data.repository

import com.willowtree.vocable.data.room.CategorySortOrder
import com.willowtree.vocable.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface PresetCategoriesRepository {
    fun getPresetCategories(): Flow<List<Category.PresetCategory>>
    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>)
    suspend fun getCategoryById(categoryId: String): Category.PresetCategory?
    suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean)
    suspend fun deleteCategory(categoryId: String)
}
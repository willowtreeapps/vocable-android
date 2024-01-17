package com.willowtree.vocable.room

import com.willowtree.vocable.presets.Category
import kotlinx.coroutines.flow.Flow

interface StoredCategoriesRepository {
    fun getAllCategories(): Flow<List<CategoryDto>>
    suspend fun upsertCategory(category: Category.StoredCategory)
    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>)
    suspend fun getCategoryById(categoryId: String): CategoryDto?
    suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean)
}
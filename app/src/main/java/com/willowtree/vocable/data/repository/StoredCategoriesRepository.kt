package com.willowtree.vocable.data.repository

import com.willowtree.vocable.data.room.CategoryDto
import com.willowtree.vocable.data.room.CategorySortOrder
import com.willowtree.vocable.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface StoredCategoriesRepository {
    fun getAllCategories(): Flow<List<CategoryDto>>
    suspend fun upsertCategory(category: Category.StoredCategory)
    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>)
    suspend fun getCategoryById(categoryId: String): CategoryDto?
    suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean)
    suspend fun deleteCategory(categoryId: String)
}
package com.willowtree.vocable

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.room.CategorySortOrder
import kotlinx.coroutines.flow.Flow

interface ICategoriesUseCase {
    fun categories(): Flow<List<Category>>
    suspend fun updateCategoryName(categoryId: String, localizedName: Map<String, String>)
    suspend fun addCategory(categoryName: String, sortOrder: Int)
    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>)
}
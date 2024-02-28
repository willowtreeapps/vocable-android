package com.willowtree.vocable

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow

interface ICategoriesUseCase {
    fun categories(): Flow<List<Category>>
    suspend fun updateCategoryName(categoryId: String, localizedName: LocalesWithText)
    suspend fun addCategory(categoryName: String)
    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>)
    suspend fun getCategoryById(categoryId: String): Category
    suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean)
    suspend fun deleteCategory(categoryId: String)
    suspend fun moveCategoryUp(categoryId: String)
    suspend fun moveCategoryDown(categoryId: String)
}
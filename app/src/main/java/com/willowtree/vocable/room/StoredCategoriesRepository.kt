package com.willowtree.vocable.room

import com.willowtree.vocable.presets.Category
import kotlinx.coroutines.flow.Flow

interface StoredCategoriesRepository {
    fun getAllCategories(): Flow<List<CategoryDto>>
    suspend fun addCategory(category: Category.StoredCategory)
}
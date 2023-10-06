package com.willowtree.vocable.room

import kotlinx.coroutines.flow.Flow

interface StoredCategoriesRepository {
    fun getAllCategories(): Flow<List<CategoryDto>>
}
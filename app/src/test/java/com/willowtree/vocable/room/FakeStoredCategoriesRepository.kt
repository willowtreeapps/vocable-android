package com.willowtree.vocable.room

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeStoredCategoriesRepository : StoredCategoriesRepository {
    val _allCategories = MutableStateFlow(listOf(CategoryDto(
        "categoryId",
        0L,
        0,
        emptyMap(),
        false,
        0
    )))

    override fun getAllCategories(): Flow<List<CategoryDto>> {
        return _allCategories
    }
}
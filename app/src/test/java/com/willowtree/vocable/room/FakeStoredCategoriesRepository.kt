package com.willowtree.vocable.room

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeStoredCategoriesRepository : StoredCategoriesRepository {
    val _allCategories = MutableStateFlow(
        listOf(
            CategoryDto(
                "categoryId",
                0L,
                0,
                LocalesWithText(emptyMap()),
                false,
                0
            )
        )
    )

    override fun getAllCategories(): Flow<List<CategoryDto>> {
        return _allCategories
    }

    override suspend fun addCategory(category: Category.StoredCategory) {
        _allCategories.update {
            it + CategoryDto(
                category.categoryId,
                0L,
                category.resourceId,
                category.localizedName,
                category.hidden,
                category.sortOrder
            )
        }
    }
}
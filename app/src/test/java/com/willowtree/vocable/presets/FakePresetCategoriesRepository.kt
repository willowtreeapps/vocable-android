package com.willowtree.vocable.presets

import com.willowtree.vocable.room.CategorySortOrder

class FakePresetCategoriesRepository : PresetCategoriesRepository {

    var _presetCategories = listOf(
        Category.PresetCategory(
            "presetCategory",
            0,
            false,
            0
        )
    )

    override suspend fun getPresetCategories(): List<Category.PresetCategory> {
        return _presetCategories
    }

    override suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>) {
        _presetCategories = _presetCategories.map { categoryDto ->
            val sortOrderUpdate =
                categorySortOrders.firstOrNull { it.categoryId == categoryDto.categoryId }
            if (sortOrderUpdate != null) {
                categoryDto.copy(sortOrder = sortOrderUpdate.sortOrder)
            } else {
                categoryDto
            }
        }
    }

    override suspend fun getCategoryById(categoryId: String): Category.PresetCategory {
        return _presetCategories.first { it.categoryId == categoryId }
    }
}
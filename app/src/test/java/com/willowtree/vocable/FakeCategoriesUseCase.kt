package com.willowtree.vocable

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.room.CategorySortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeCategoriesUseCase : ICategoriesUseCase {

    val _categories = MutableStateFlow(
        listOf(
            Category.StoredCategory(
                "categoryId",
                0,
                null,
                false,
                0
            )
        )
    )

    override fun categories(): Flow<List<Category>> {
        return _categories
    }

    override suspend fun updateCategoryName(
        categoryId: String,
        localizedName: Map<String, String>
    ) {
        _categories.update { categories ->
            categories.map {
                if (it.categoryId == categoryId) {
                    it.copy(localizedName = localizedName)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun addCategory(categoryName: String, sortOrder: Int) {
        _categories.update {
            it + Category.StoredCategory(
                "",
                null,
                mapOf("en_US" to categoryName),
                false,
                sortOrder
            )
        }
    }

    override suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>) {
        _categories.update { allCategories ->
            allCategories.map { categoryDto ->
                val sortOrderUpdate =
                    categorySortOrders.firstOrNull { it.categoryId == categoryDto.categoryId }
                if (sortOrderUpdate != null) {
                    categoryDto.copy(sortOrder = sortOrderUpdate.sortOrder)
                } else {
                    categoryDto
                }
            }
        }
    }
}
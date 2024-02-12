package com.willowtree.vocable

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Deprecated("This fake is too complex, tests using it should migrate to integration tests with" +
        "the actual data sources.")
class FakeCategoriesUseCase : ICategoriesUseCase {

    val _categories = MutableStateFlow<List<Category>>(
        listOf(
            Category.StoredCategory(
                "categoryId",
                LocalesWithText(mapOf("en_US" to "storedCategory")),
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
        localizedName: LocalesWithText
    ) {
        _categories.update { categories ->
            categories.map {
                if (it.categoryId == categoryId) {
                    when (it) {
                        is Category.StoredCategory -> it.copy(localizedName = localizedName)
                        is Category.PresetCategory -> Category.StoredCategory(
                            it.categoryId,
                            localizedName,
                            it.hidden,
                            it.sortOrder
                        )

                        is Category.Recents -> it
                    }
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
                LocalesWithText(mapOf("en_US" to categoryName)),
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
                    when(categoryDto) {
                        is Category.StoredCategory -> categoryDto.copy(sortOrder = sortOrderUpdate.sortOrder)
                        is Category.PresetCategory -> categoryDto.copy(sortOrder = sortOrderUpdate.sortOrder)
                        is Category.Recents -> categoryDto.copy(sortOrder = sortOrderUpdate.sortOrder)
                    }
                } else {
                    categoryDto
                }
            }
        }
    }

    override suspend fun getCategoryById(categoryId: String): Category {
        return _categories.value.first { it.categoryId == categoryId }
    }

    override suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean) {
        _categories.update { categories ->
            categories.map {
                if (it.categoryId == categoryId) {
                    when(it) {
                        is Category.StoredCategory -> it.copy(hidden = hidden)
                        is Category.PresetCategory -> it.copy(hidden = hidden)
                        is Category.Recents -> it.copy(hidden = hidden)
                    }
                } else {
                    it
                }
            }
        }
    }

    override suspend fun deleteCategory(categoryId: String) {
        _categories.update { categories ->
            categories.filter { it.categoryId != categoryId }
        }
    }
}
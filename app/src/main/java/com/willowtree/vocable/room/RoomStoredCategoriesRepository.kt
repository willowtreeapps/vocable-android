package com.willowtree.vocable.room

import com.willowtree.vocable.presets.Category
import kotlinx.coroutines.flow.Flow

class RoomStoredCategoriesRepository(
    private val database: VocableDatabase
) : StoredCategoriesRepository {

    override fun getAllCategories(): Flow<List<CategoryDto>> {
        return database.categoryDao().getAllCategoriesFlow()
    }

    override suspend fun upsertCategory(category: Category.StoredCategory) {
        database.categoryDao().insertCategory(
            CategoryDto(
                category.categoryId,
                0L,
                category.resourceId,
                category.localizedName,
                category.hidden,
                category.sortOrder
            )
        )
    }

    override suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>) {
        database.categoryDao().updateCategorySortOrders(categorySortOrders)
    }

    override suspend fun getCategoryById(categoryId: String): CategoryDto? =
        database.categoryDao().getCategoryById(categoryId)

    override suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean) {
        database.categoryDao().updateCategoryHidden(StoredCategoryHidden(categoryId, hidden))
    }

    override suspend fun deleteCategory(categoryId: String) {
        database.categoryDao().deleteCategory(categoryId)
    }
}
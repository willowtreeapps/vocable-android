package com.willowtree.vocable.room

import android.content.Context
import com.willowtree.vocable.presets.Category
import kotlinx.coroutines.flow.Flow

class RoomStoredCategoriesRepository(context: Context) : StoredCategoriesRepository {
    private val database = VocableDatabase.getVocableDatabase(context)

    override fun getAllCategories(): Flow<List<CategoryDto>> {
        return database.categoryDao().getAllCategoriesFlow()
    }

    override suspend fun addCategory(category: Category.StoredCategory) {
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
}
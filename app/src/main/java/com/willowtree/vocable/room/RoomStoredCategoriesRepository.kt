package com.willowtree.vocable.room

import android.content.Context
import kotlinx.coroutines.flow.Flow

class RoomStoredCategoriesRepository(context: Context) : StoredCategoriesRepository {
    private val database = VocableDatabase.getVocableDatabase(context)

    override fun getAllCategories(): Flow<List<CategoryDto>> {
        return database.categoryDao().getAllCategoriesFlow()
    }
}
package com.willowtree.vocable.presets

import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.room.PresetCategoryDeleted
import com.willowtree.vocable.room.PresetCategoryDto
import com.willowtree.vocable.room.PresetCategoryHidden
import com.willowtree.vocable.room.VocableDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomPresetCategoriesRepository(
    private val database: VocableDatabase
) : PresetCategoriesRepository {
    // Needed because we're checking database contents and making updates based
    // on them. Multiple callers may call at once.
    private val categoryMutex = Mutex()

    override fun getPresetCategories(): Flow<List<Category.PresetCategory>> {
        return database.presetCategoryDao().getAllPresetCategoriesFlow().map { presetCategories ->
            presetCategories.filterNot { it.deleted }
                .map {
                    Category.PresetCategory(
                        it.categoryId,
                        it.sortOrder,
                        it.hidden,
                        PresetCategories.values()
                            .first { presetCategory -> presetCategory.id == it.categoryId }
                            .getNameId()
                    )
                }
        }.onStart { ensurePopulated() }
    }

    private suspend fun ensurePopulated() {
        categoryMutex.withLock {
            val dbPresets = database.presetCategoryDao().getAllPresetCategoriesFlow().first()
            PresetCategories.values().filter { it != PresetCategories.MY_SAYINGS }
                .filter { presetCategory -> dbPresets.none { it.categoryId == presetCategory.id } }
                .map { presetCategory -> populatePresetCategory(presetCategory.id) }
        }
    }

    private suspend fun populatePresetCategory(categoryId: String) {
        val presetCategory = PresetCategories.values().firstOrNull { it.id == categoryId } ?: error(
            "Unknown preset category id: $categoryId"
        )
        val newPresetDto = PresetCategoryDto(
            categoryId = categoryId,
            hidden = false,
            sortOrder = presetCategory.initialSortOrder,
            deleted = false
        )
        database.presetCategoryDao().insertPresetCategory(newPresetDto)
    }

    override suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>) {
        ensurePopulated()
        database.presetCategoryDao().updateCategorySortOrders(categorySortOrders)
    }

    override suspend fun getCategoryById(categoryId: String): Category.PresetCategory? {
        ensurePopulated()
        return database.presetCategoryDao().getPresetCategoryById(categoryId)?.let {
            Category.PresetCategory(
                it.categoryId,
                it.sortOrder,
                it.hidden,
                PresetCategories.values()
                    .first { presetCategory -> presetCategory.id == it.categoryId }
                    .getNameId()
            )
        }
    }

    override suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean) {
        ensurePopulated()
        database.presetCategoryDao().updateCategoryHidden(PresetCategoryHidden(categoryId, hidden))
    }

    override suspend fun deleteCategory(categoryId: String) {
        ensurePopulated()
        database.presetCategoryDao().updateCategoryDeleted(PresetCategoryDeleted(categoryId, true))
    }
}
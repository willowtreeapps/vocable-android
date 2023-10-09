package com.willowtree.vocable.presets

interface PresetCategoriesRepository {
    suspend fun getPresetCategories(): List<Category.PresetCategory>
}
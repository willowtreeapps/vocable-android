package com.willowtree.vocable.presets

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
}
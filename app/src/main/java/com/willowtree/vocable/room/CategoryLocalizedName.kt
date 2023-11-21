package com.willowtree.vocable.room

import androidx.room.ColumnInfo
import com.willowtree.vocable.utils.locale.LocalesWithText

data class CategoryLocalizedName(
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "localized_name") var localizedName: LocalesWithText
)
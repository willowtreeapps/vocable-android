package com.willowtree.vocable.data.room

import androidx.room.ColumnInfo
import com.willowtree.vocable.core.locale.LocalesWithText

data class CategoryLocalizedName(
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "localized_name") var localizedName: LocalesWithText
)
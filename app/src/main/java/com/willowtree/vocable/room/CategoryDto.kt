package com.willowtree.vocable.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.parcelize.Parcelize

@Entity(tableName = "Category")
@Parcelize
data class CategoryDto(
    @PrimaryKey @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "creation_date") val creationDate: Long,
    @ColumnInfo(name = "localized_name") var localizedName: LocalesWithText,
    var hidden: Boolean,
    @ColumnInfo(name = "sort_order") var sortOrder: Int
) : Parcelable
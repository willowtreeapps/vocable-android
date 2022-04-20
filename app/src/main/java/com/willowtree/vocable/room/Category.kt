package com.willowtree.vocable.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class Category(
    @PrimaryKey @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "creation_date") val creationDate: Long,
    @ColumnInfo(name = "resource_id") val resourceId: Int?,
    @ColumnInfo(name = "localized_name") var localizedName: Map<String, String>?,
    var hidden: Boolean,
    @ColumnInfo(name = "sort_order") var sortOrder: Int
) : Parcelable
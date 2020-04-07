package com.willowtree.vocable.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.willowtree.vocable.utils.LocaleUtils
import kotlinx.android.parcel.Parcelize

@Entity
@Parcelize
data class Category(
    @PrimaryKey @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "creation_date") val creationDate: Long,
    @ColumnInfo(name = "is_user_generated") val isUserGenerated: Boolean,
    @ColumnInfo(name = "localized_name") val localizedName: Map<String, String>,
    var hidden: Boolean,
    @ColumnInfo(name = "sort_order") var sortOrder: Int
) : Parcelable {

    fun getLocalizedText(): String {
        return LocaleUtils.getTextForLocale(localizedName)
    }
}
package com.willowtree.vocable.presets

import android.os.Parcelable
import com.willowtree.vocable.room.CategoryDto
import kotlinx.parcelize.Parcelize

sealed class Category : Parcelable {
    abstract val categoryId: String
    abstract val sortOrder: Int
    abstract val hidden: Boolean
    abstract val localizedName: Map<String, String>?
    abstract val resourceId: Int?

    abstract fun withSortOrder(sortOrder: Int): Category
    abstract fun withLocalizedName(localizedName: Map<String, String>?): Category
    abstract fun withHidden(hidden: Boolean): Category

    @Parcelize
    data class StoredCategory(
        override val categoryId: String,
        override val resourceId: Int?,
        override val localizedName: Map<String, String>?,
        override var hidden: Boolean,
        override var sortOrder: Int
    ) : Category() {
        override fun withSortOrder(sortOrder: Int): Category = copy(sortOrder = sortOrder)
        override fun withLocalizedName(localizedName: Map<String, String>?): Category =
            copy(localizedName = localizedName)

        override fun withHidden(hidden: Boolean): Category = copy(hidden = hidden)
    }

    @Parcelize
    data class Recents(
        override val resourceId: Int?,
        override val localizedName: Map<String, String>?,
        override val hidden: Boolean,
        override val sortOrder: Int
    ) : Category() {
        override val categoryId: String = PresetCategories.RECENTS.id
        override fun withSortOrder(sortOrder: Int): Category = copy(sortOrder = sortOrder)
        override fun withLocalizedName(localizedName: Map<String, String>?): Category =
            copy(localizedName = localizedName)

        override fun withHidden(hidden: Boolean): Category = copy(hidden = hidden)
    }
}

fun CategoryDto.asCategory(): Category {
    return if (categoryId == PresetCategories.RECENTS.id) {
        Category.Recents(
            resourceId = resourceId,
            localizedName = localizedName,
            hidden = hidden,
            sortOrder = sortOrder
        )
    } else {
        Category.StoredCategory(
            categoryId,
            resourceId,
            localizedName,
            hidden,
            sortOrder
        )
    }
}
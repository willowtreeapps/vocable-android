package com.willowtree.vocable.presets

import android.os.Parcelable
import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.parcelize.Parcelize

sealed class Category : Parcelable {
    abstract val categoryId: String
    abstract val sortOrder: Int
    abstract val hidden: Boolean
    abstract val localizedName: LocalesWithText?
    abstract val resourceId: Int?

    abstract fun withSortOrder(sortOrder: Int): Category
    abstract fun withHidden(hidden: Boolean): Category

    @Parcelize
    data class StoredCategory(
        override val categoryId: String,
        override val localizedName: LocalesWithText?,
        override var hidden: Boolean,
        override var sortOrder: Int
    ) : Category() {
        override val resourceId: Int? = null
        override fun withSortOrder(sortOrder: Int): Category = copy(sortOrder = sortOrder)
        override fun withHidden(hidden: Boolean): Category = copy(hidden = hidden)
    }

    @Parcelize
    data class Recents(
        override val resourceId: Int?,
        override val localizedName: LocalesWithText?,
        override val hidden: Boolean,
        override val sortOrder: Int
    ) : Category() {
        override val categoryId: String = PresetCategories.RECENTS.id
        override fun withSortOrder(sortOrder: Int): Category = copy(sortOrder = sortOrder)
        override fun withHidden(hidden: Boolean): Category = copy(hidden = hidden)
    }

    @Parcelize
    data class PresetCategory(
        override val categoryId: String,
        override val sortOrder: Int,
        override val hidden: Boolean,
        override val resourceId: Int
    ) : Category() {
        override val localizedName: LocalesWithText? = null
        override fun withSortOrder(sortOrder: Int): Category = copy(sortOrder = sortOrder)
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
            localizedName,
            hidden,
            sortOrder
        )
    }
}
package com.willowtree.vocable.presets

import android.content.Context
import android.os.Parcelable
import com.willowtree.vocable.R
import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.utils.locale.LocalesWithText
import com.willowtree.vocable.utils.locale.text
import kotlinx.parcelize.Parcelize

sealed class Category : Parcelable {
    abstract val categoryId: String
    abstract val sortOrder: Int
    abstract val hidden: Boolean

    abstract fun withSortOrder(sortOrder: Int): Category
    abstract fun withHidden(hidden: Boolean): Category
    abstract fun text(context: Context): String

    @Parcelize
    data class StoredCategory(
        override val categoryId: String,
        val localizedName: LocalesWithText,
        override var hidden: Boolean,
        override var sortOrder: Int
    ) : Category() {
        override fun withSortOrder(sortOrder: Int): Category = copy(sortOrder = sortOrder)
        override fun withHidden(hidden: Boolean): Category = copy(hidden = hidden)
        override fun text(context: Context): String {
            return localizedName.localizedText.text()
        }
    }

    @Parcelize
    data class Recents(
        override val hidden: Boolean,
        override val sortOrder: Int
    ) : Category() {
        override val categoryId: String = PresetCategories.RECENTS.id
        override fun withSortOrder(sortOrder: Int): Category = copy(sortOrder = sortOrder)
        override fun withHidden(hidden: Boolean): Category = copy(hidden = hidden)
        override fun text(context: Context): String {
            return context.getString(R.string.preset_recents)
        }
    }

    @Parcelize
    data class PresetCategory(
        override val categoryId: String,
        override val sortOrder: Int,
        override val hidden: Boolean
    ) : Category() {
        override fun withSortOrder(sortOrder: Int): Category = copy(sortOrder = sortOrder)
        override fun withHidden(hidden: Boolean): Category = copy(hidden = hidden)
        override fun text(context: Context): String {
            val categoryStringRes = context.resources.getIdentifier(
                /* name = */ categoryId,
                /* defType = */ "string",
                /* defPackage = */ context.packageName
            )
            return context.resources.getString(categoryStringRes)
        }
    }
}

fun CategoryDto.asCategory(): Category {
    return if (categoryId == PresetCategories.RECENTS.id) {
        Category.Recents(
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
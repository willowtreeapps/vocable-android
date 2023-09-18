package com.willowtree.vocable.presets

import android.os.Parcelable
import com.willowtree.vocable.room.CategoryDto
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val categoryId: String,
    val creationDate: Long,
    val resourceId: Int?,
    val localizedName: Map<String, String>?,
    var hidden: Boolean,
    var sortOrder: Int
) : Parcelable

fun CategoryDto.asCategory(): Category = Category(
    categoryId,
    creationDate,
    resourceId,
    localizedName,
    hidden,
    sortOrder
)

fun Category.asDto(): CategoryDto = CategoryDto(
    categoryId,
    creationDate,
    resourceId,
    localizedName,
    hidden,
    sortOrder
)
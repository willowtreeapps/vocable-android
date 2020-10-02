package com.willowtree.vocable.room

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["category_id", "phrase_id"])
data class CategoryPhraseCrossRef(
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "phrase_id") val phraseId: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long? = null
)
package com.willowtree.vocable.room

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class CategoryWithPhrases(
    @Embedded val category: Category,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "parent_category_id"
    )
    val phrases: List<Phrase>?
)
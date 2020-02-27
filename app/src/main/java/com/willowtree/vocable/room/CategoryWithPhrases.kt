package com.willowtree.vocable.room

import androidx.room.Embedded
import androidx.room.Relation

data class CategoryWithPhrases(
    @Embedded val category: Category,
    @Relation(
        parentColumn = "identifier",
        entityColumn = "category_id"
    )
    val phrases: List<Phrase>
)
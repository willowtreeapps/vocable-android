package com.willowtree.vocable.room

import androidx.room.Embedded
import androidx.room.Relation

data class CategoryWithPhrases(
    @Embedded val category: CategoryDto,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "parent_category_id"
    )
    val phrases: List<PhraseDto>?
)
package com.willowtree.vocable.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.parcelize.Parcelize

@Entity(tableName = "Phrase")
@Parcelize
data class PhraseDto(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "phrase_id") val phraseId: Long,
    @ColumnInfo(name = "parent_category_id") val parentCategoryId: String?,
    @ColumnInfo(name = "creation_date") val creationDate: Long,
    @ColumnInfo(name = "last_spoken_date") val lastSpokenDate: Long?,
    @ColumnInfo(name = "localized_utterance") val localizedUtterance: LocalesWithText?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @Ignore
    @ColumnInfo(name = "utterance_string_res") val utteranceStringRes: Int? = null,
) : Parcelable {

    // TODO: Remove this when we un-ignore [utteranceStringRes]. This constructor is needed for
    //       Room to be able to instantiate the class b/c it doesn't know about utteranceStringRes
    constructor(
        phraseId: Long,
        parentCategoryId: String?,
        creationDate: Long,
        lastSpokenDate: Long?,
        localizedUtterance: LocalesWithText?,
        sortOrder: Int,
    ) : this(
        phraseId = phraseId,
        parentCategoryId = parentCategoryId,
        creationDate = creationDate,
        lastSpokenDate = lastSpokenDate,
        localizedUtterance = localizedUtterance,
        sortOrder = sortOrder,
        utteranceStringRes = null,
    )
}
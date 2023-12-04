package com.willowtree.vocable.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
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
    @ColumnInfo(name = "localized_utterance") var localizedUtterance: LocalesWithText?,
    @ColumnInfo(name = "sort_order") var sortOrder: Int
) : Parcelable
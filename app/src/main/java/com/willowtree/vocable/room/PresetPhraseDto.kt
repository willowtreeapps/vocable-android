package com.willowtree.vocable.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "PresetPhrase")
@Parcelize
data class PresetPhraseDto(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "phrase_id") val phraseId: Long,
    @ColumnInfo(name = "parent_category_id") val parentCategoryId: String?,
    @ColumnInfo(name = "creation_date") val creationDate: Long,
    @ColumnInfo(name = "last_spoken_date") val lastSpokenDate: Long?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "utterance_string_res") val utteranceStringRes: Int,
) : Parcelable
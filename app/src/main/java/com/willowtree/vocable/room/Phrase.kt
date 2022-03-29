package com.willowtree.vocable.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.willowtree.vocable.utils.LocaleUtils
import kotlinx.android.parcel.Parcelize
import java.util.*

@Entity
@Parcelize
data class Phrase(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "phrase_id") val phraseId: Long,
    @ColumnInfo(name = "parent_category_id") val parentCategoryId: String,
    @ColumnInfo(name = "creation_date") val creationDate: Long,
    @ColumnInfo(name = "is_user_generated") val isUserGenerated: Boolean,
    @ColumnInfo(name = "last_spoken_date") val lastSpokenDate: Long,
    @ColumnInfo(name = "resource_id") val resourceId: Int?,
    @ColumnInfo(name = "localized_utterance") var localizedUtterance: Map<String, String>?,
    @ColumnInfo(name = "sort_order") var sortOrder: Int
) : Parcelable
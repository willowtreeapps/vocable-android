package com.willowtree.vocable.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(indices = [Index(value = ["utterance"], unique = true)])
@Parcelize
data class Phrase(
    @PrimaryKey val identifier: Long,
    @ColumnInfo(name = "creation_date") val creationDate: Long,
    @ColumnInfo(name = "is_user_generated") val isUserGenerated: Boolean,
    @ColumnInfo(name = "last_spoken_date") val lastSpokenDate: Long,
    val utterance: String,
    @ColumnInfo(name = "category_id") val categoryId: Long
) : Parcelable
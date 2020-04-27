package com.willowtree.vocable.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.willowtree.vocable.utils.LocaleUtils
import kotlinx.android.parcel.Parcelize
import java.util.*

@Entity
@Parcelize
data class Phrase(
    @PrimaryKey @ColumnInfo(name = "phrase_id") val phraseId: String,
    @ColumnInfo(name = "creation_date") val creationDate: Long,
    @ColumnInfo(name = "is_user_generated") val isUserGenerated: Boolean,
    @ColumnInfo(name = "last_spoken_date") val lastSpokenDate: Long,
    @ColumnInfo(name = "localized_utterance") var localizedUtterance: Map<String, String>,
    @ColumnInfo(name = "sort_order") var sortOrder: Int
) : Parcelable {

    fun getLocalizedText(): String {
        return LocaleUtils.getTextForLocale(localizedUtterance)
    }

    fun getLocalizedPair(): Pair<String, Locale> {
        return LocaleUtils.getLocalizedPair(localizedUtterance)
    }
}
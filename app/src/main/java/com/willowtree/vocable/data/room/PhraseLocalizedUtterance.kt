package com.willowtree.vocable.data.room

import androidx.room.ColumnInfo
import com.willowtree.vocable.core.locale.LocalesWithText

data class PhraseLocalizedUtterance(
    @ColumnInfo(name = "phrase_id") val phraseId: String,
    @ColumnInfo(name = "localized_utterance") var localizedUtterance: LocalesWithText
)

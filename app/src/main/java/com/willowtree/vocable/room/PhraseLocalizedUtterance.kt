package com.willowtree.vocable.room

import androidx.room.ColumnInfo
import com.willowtree.vocable.utils.locale.LocalesWithText

data class PhraseLocalizedUtterance(
    @ColumnInfo(name = "phrase_id") val phraseId: String,
    @ColumnInfo(name = "localized_utterance") var localizedUtterance: LocalesWithText
)

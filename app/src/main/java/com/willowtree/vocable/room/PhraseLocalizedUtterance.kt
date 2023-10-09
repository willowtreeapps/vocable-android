package com.willowtree.vocable.room

import androidx.room.ColumnInfo

data class PhraseLocalizedUtterance(
    @ColumnInfo(name = "phrase_id") val phraseId: Long,
    @ColumnInfo(name = "localized_utterance") var localizedUtterance: Map<String, String>
)

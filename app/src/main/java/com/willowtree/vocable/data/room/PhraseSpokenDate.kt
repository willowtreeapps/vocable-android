package com.willowtree.vocable.data.room

import androidx.room.ColumnInfo

data class PhraseSpokenDate(
    @ColumnInfo(name = "phrase_id") val phraseId: String,
    @ColumnInfo(name = "last_spoken_date") val lastSpokenDate: Long
)

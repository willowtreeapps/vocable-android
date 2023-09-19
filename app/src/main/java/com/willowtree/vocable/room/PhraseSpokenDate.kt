package com.willowtree.vocable.room

import androidx.room.ColumnInfo

data class PhraseSpokenDate(
    @ColumnInfo(name = "phrase_id") val phraseId: Long,
    @ColumnInfo(name = "last_spoken_date") val lastSpokenDate: Long
)

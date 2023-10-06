package com.willowtree.vocable.presets

import android.os.Parcelable
import com.willowtree.vocable.room.PhraseDto
import kotlinx.parcelize.Parcelize

@Parcelize
data class Phrase(
    val phraseId: Long,
    val localizedUtterance: Map<String, String>?,
    val sortOrder: Int
) : Parcelable

fun PhraseDto.asPhrase(): Phrase =
    Phrase(
        phraseId,
        localizedUtterance,
        sortOrder
    )
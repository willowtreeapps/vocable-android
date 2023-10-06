package com.willowtree.vocable.presets

import android.os.Parcelable
import com.willowtree.vocable.room.PhraseDto
import kotlinx.parcelize.Parcelize

sealed class Phrase : Parcelable {
    abstract val phraseId: Long
    abstract val localizedUtterance: Map<String, String>?
    abstract val sortOrder: Int
}

@Parcelize
data class CustomPhrase(
    override val phraseId: Long,
    override val localizedUtterance: Map<String, String>?,
    override val sortOrder: Int
) : Phrase(), Parcelable

fun PhraseDto.asPhrase(): Phrase =
    CustomPhrase(
        phraseId,
        localizedUtterance,
        sortOrder
    )
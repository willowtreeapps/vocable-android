package com.willowtree.vocable.presets

import android.os.Parcelable
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.parcelize.Parcelize

sealed class Phrase : Parcelable {
    abstract val phraseId: Long
    abstract val localizedUtterance: LocalesWithText?
    abstract val sortOrder: Int
}

@Parcelize
data class CustomPhrase(
    override val phraseId: Long,
    override val localizedUtterance: LocalesWithText?,
    override val sortOrder: Int
) : Phrase(), Parcelable

fun PhraseDto.asPhrase(): Phrase =
    CustomPhrase(
        phraseId,
        localizedUtterance,
        sortOrder
    )
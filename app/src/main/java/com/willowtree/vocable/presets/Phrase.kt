package com.willowtree.vocable.presets

import android.os.Parcelable
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

sealed class Phrase : Parcelable {
    abstract val phraseId: Long
    abstract val localizedUtterance: LocalesWithText?
    abstract val utteranceStringRes: Int?
    abstract val sortOrder: Int
}

@Parcelize
data class CustomPhrase(
    override val phraseId: Long,
    override val localizedUtterance: LocalesWithText?,
    override val sortOrder: Int,
) : Phrase(), Parcelable {
    @IgnoredOnParcel
    override val utteranceStringRes: Int? = null
}

@Parcelize
data class PresetPhrase(
    override val phraseId: Long,
    override val sortOrder: Int,
    override val utteranceStringRes: Int?,
) : Phrase() {
    @IgnoredOnParcel
    override val localizedUtterance: LocalesWithText? = null
}

fun PhraseDto.asPhrase(): Phrase {
    return if (utteranceStringRes != null) {
        PresetPhrase(
            phraseId = phraseId,
            sortOrder = sortOrder,
            utteranceStringRes = utteranceStringRes
        )
    } else if (localizedUtterance != null) {
        CustomPhrase(
            phraseId = phraseId,
            localizedUtterance = localizedUtterance,
            sortOrder = sortOrder
        )
    } else {
        throw IllegalStateException("Phrase must have either localizedUtterance or utteranceStringRes")
    }
}
package com.willowtree.vocable.presets

import android.content.res.Resources
import android.os.Parcelable
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.utils.locale.LocalesWithText
import com.willowtree.vocable.utils.locale.text
import kotlinx.parcelize.Parcelize

sealed class Phrase : Parcelable {
    abstract val phraseId: Long
    abstract val sortOrder: Int

    abstract fun text(resources: Resources): String
}

@Parcelize
data class CustomPhrase(
    override val phraseId: Long,
    override val sortOrder: Int,
    val localizedUtterance: LocalesWithText?,
) : Phrase(), Parcelable {

    override fun text(resources: Resources): String {
        return localizedUtterance?.localizedText?.text() ?: ""
    }
}

@Parcelize
data class PresetPhrase(
    override val phraseId: Long,
    override val sortOrder: Int,
    val utteranceStringRes: Int?,
) : Phrase() {

    override fun text(resources: Resources): String {
        return resources.getString(utteranceStringRes ?: 0)
    }
}

fun PhraseDto.asPhrase(): Phrase =
    CustomPhrase(
        phraseId,
        sortOrder,
        localizedUtterance,
    )
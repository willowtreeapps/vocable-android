package com.willowtree.vocable.presets

import android.content.Context
import android.os.Parcelable
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.room.PresetPhraseDto
import com.willowtree.vocable.utils.locale.LocalesWithText
import com.willowtree.vocable.utils.locale.text
import kotlinx.parcelize.Parcelize

sealed interface Phrase : Parcelable {
    val phraseId: String
    val sortOrder: Int
    val lastSpokenDate: Long?
    fun text(context: Context): String
}

@Parcelize
data class CustomPhrase(
    override val phraseId: String,
    override val sortOrder: Int,
    val localizedUtterance: LocalesWithText?,
    override val lastSpokenDate: Long?,
) : Phrase, Parcelable {

    override fun text(context: Context): String {
        return localizedUtterance?.localizedText?.text() ?: ""
    }
}

@Parcelize
data class PresetPhrase(
    override val phraseId: String,
    override val sortOrder: Int,
    override val lastSpokenDate: Long?,
) : Phrase {

    override fun text(context: Context): String {
        val utteranceStringRes = context.resources.getIdentifier(
            /* name = */ phraseId,
            /* defType = */ "string",
            /* defPackage = */ context.packageName
        )
        return context.resources.getString(utteranceStringRes)
    }
}

fun PhraseDto.asPhrase(): Phrase =
    CustomPhrase(
        phraseId = phraseId.toString(),
        sortOrder = sortOrder,
        localizedUtterance = localizedUtterance,
        lastSpokenDate = lastSpokenDate,
    )

fun PresetPhraseDto.asPhrase(): PresetPhrase =
    PresetPhrase(
        phraseId = phraseId,
        sortOrder = sortOrder,
        lastSpokenDate = lastSpokenDate,
    )

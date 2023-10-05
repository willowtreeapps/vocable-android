package com.willowtree.vocable.presets

import android.os.Parcelable
import com.willowtree.vocable.room.PhraseDto
import kotlinx.parcelize.Parcelize

@Parcelize
data class Phrase(
    val phraseId: Long,
    val parentCategoryId: String?,
    val creationDate: Long,
    val lastSpokenDate: Long?,
    val localizedUtterance: Map<String, String>?,
    val sortOrder: Int
) : Parcelable

fun Phrase.asDto(): PhraseDto =
    PhraseDto(
        phraseId,
        parentCategoryId,
        creationDate,
        lastSpokenDate,
        localizedUtterance,
        sortOrder
    )

fun PhraseDto.asPhrase(): Phrase =
    Phrase(
        phraseId,
        parentCategoryId,
        creationDate,
        lastSpokenDate,
        localizedUtterance,
        sortOrder
    )
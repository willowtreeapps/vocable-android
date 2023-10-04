package com.willowtree.vocable.presets

import com.willowtree.vocable.room.PhraseDto

data class Phrase(
    val phraseId: Long,
    val parentCategoryId: String?,
    val creationDate: Long,
    val lastSpokenDate: Long?,
    val localizedUtterance: Map<String, String>?,
    val sortOrder: Int
)

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
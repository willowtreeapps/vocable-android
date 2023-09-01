package com.willowtree.vocable.presets

import com.willowtree.vocable.room.Phrase

//TODO: PK - Rename this once we make the jump to rename [PresetsRepository] -> "RoomPresetsRepository"
interface IPresetsRepository {
    suspend fun getPhrasesForCategory(categoryId: String): List<Phrase>
    suspend fun addPhraseToRecents(phrase: Phrase)
}
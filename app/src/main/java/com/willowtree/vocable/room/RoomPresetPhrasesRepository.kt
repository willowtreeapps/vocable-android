package com.willowtree.vocable.room

class RoomPresetPhrasesRepository(
    private val database: VocableDatabase
) : PresetPhrasesRepository {
    override suspend fun addPhrase(phrase: PresetPhraseDto) {
        database.presetPhrasesDao().insertPhrase(phrase)
    }
}
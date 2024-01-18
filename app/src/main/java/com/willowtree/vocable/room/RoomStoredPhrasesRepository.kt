package com.willowtree.vocable.room

class RoomStoredPhrasesRepository(
    private val database: VocableDatabase
) : StoredPhrasesRepository {
    override suspend fun addPhrase(phrase: PhraseDto) {
        database.phraseDao().insertPhrase(phrase)
    }
}
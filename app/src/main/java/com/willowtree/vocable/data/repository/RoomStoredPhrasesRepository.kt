package com.willowtree.vocable.data.repository

import com.willowtree.vocable.data.room.PhraseDto
import com.willowtree.vocable.data.room.PhraseLocalizedUtterance
import com.willowtree.vocable.data.room.PhraseSpokenDate
import com.willowtree.vocable.data.room.VocableDatabase
import com.willowtree.vocable.domain.model.Phrase
import com.willowtree.vocable.domain.model.asPhrase
import com.willowtree.vocable.core.DateProvider
import com.willowtree.vocable.core.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomStoredPhrasesRepository(
    private val database: VocableDatabase,
    private val dateProvider: DateProvider,
) : StoredPhrasesRepository {
    override suspend fun addPhrase(phrase: PhraseDto) {
        database.phraseDao().insertPhrase(phrase)
    }

    override suspend fun updatePhraseLastSpokenTime(phraseId: String) {
        database.phraseDao().updatePhraseSpokenDate(
            PhraseSpokenDate(
                phraseId = phraseId,
                lastSpokenDate = dateProvider.currentTimeMillis()
            )
        )
    }

    override fun getRecentPhrasesFlow(): Flow<List<Phrase>> {
        return database.phraseDao().getRecentPhrases()
            .map { phraseList -> phraseList.map { it.asPhrase() } }
    }

    override fun getPhrasesForCategoryFlow(categoryId: String): Flow<List<Phrase>> {
        return database.phraseDao().getPhrasesForCategory(categoryId)
            .map { phraseList -> phraseList.map { it.asPhrase() } }
    }

    override suspend fun getPhrase(phraseId: String): Phrase? {
        return database.phraseDao().getPhrase(phraseId)?.asPhrase()
    }

    override suspend fun updatePhrase(phrase: PhraseDto) {
        database.phraseDao().insertPhrase(phrase)
    }

    override suspend fun updatePhraseLocalizedUtterance(
        phraseId: String,
        localizedUtterance: LocalesWithText
    ) {
        database.phraseDao().updatePhraseLocalizedUtterance(
            PhraseLocalizedUtterance(
                phraseId = phraseId,
                localizedUtterance = localizedUtterance
            )
        )
    }

    override suspend fun deletePhrase(phraseId: String) {
        database.phraseDao().deletePhrase(phraseId)
    }
}
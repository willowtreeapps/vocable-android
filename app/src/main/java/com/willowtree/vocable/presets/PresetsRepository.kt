package com.willowtree.vocable.presets

import android.content.Context
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.room.VocableDatabase
import org.koin.core.KoinComponent
import org.koin.core.get

class PresetsRepository(context: Context) : KoinComponent {

    private val database = VocableDatabase.getVocableDatabase(context)

    suspend fun getAllCategories(): List<Category> {
        return database.categoryDao().getAllCategories()
    }

    suspend fun getPhrasesForCategory(categoryId: Long): List<Phrase> {
        return database.phraseDao().getPhrasesByCategory(categoryId)
    }

    suspend fun getUserGeneratedPhrases(): List<Phrase> {
        return database.phraseDao().getUserGeneratedPhrases()
    }

    suspend fun addPhrase(phrase: Phrase) {
        database.phraseDao().insertPhrase(phrase)
    }

    suspend fun populateCategories(categories: List<Category>) {
        database.categoryDao().insertCategories(*categories.toTypedArray())
    }

    suspend fun populatePhrases(phrases: List<Phrase>) {
        database.phraseDao().insertPhrases(*phrases.toTypedArray())
    }

    suspend fun getMySayingsId(): Long {
        return database.categoryDao().getMySayingsId()
    }

    suspend fun deletePhrase(phrase: Phrase) {
        database.phraseDao().deletePhrase(phrase)
    }

    suspend fun updatePhrase(phrase: Phrase) {
        database.phraseDao().updatePhrase(phrase)
    }

    suspend fun updateCategory(category: Category) {
        database.categoryDao().updateCategory(category)
    }
}
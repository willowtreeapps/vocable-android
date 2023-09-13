package com.willowtree.vocable.presets

import android.content.Context
import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.room.VocableDatabase
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.Calendar
import java.util.Locale

class PresetsRepository(val context: Context) : KoinComponent, IPresetsRepository {

    private val database = VocableDatabase.getVocableDatabase(context)

    override suspend fun getAllCategories(): List<CategoryDto> {
        return database.categoryDao().getAllCategories()
    }

    override fun getAllCategoriesFlow(): Flow<List<CategoryDto>> {
        return database.categoryDao().getAllCategoriesFlow()
    }

    override suspend fun getPhrasesForCategory(categoryId: String): List<Phrase> {
        return database.categoryDao().getCategoryWithPhrases(categoryId)?.phrases ?: listOf()
    }

    suspend fun addPhrase(phrase: Phrase) {
        database.phraseDao().insertPhrase(phrase)
    }

    override suspend fun addPhraseToRecents(phrase: Phrase) {

        val phrases = getPhrasesForCategory(
            PresetCategories.RECENTS.id
        )
        val recentPhrase = phrases.firstOrNull {
            it.localizedUtterance == phrase.localizedUtterance
        }
        if (recentPhrase == null) {
            addPhrase(
                Phrase(
                    phraseId = 0L,
                    parentCategoryId = PresetCategories.RECENTS.id,
                    creationDate = Calendar.getInstance().timeInMillis,
                    lastSpokenDate = Calendar.getInstance().timeInMillis,
                    localizedUtterance = phrase.localizedUtterance,
                    sortOrder = phrase.sortOrder
                )
            )
            if (phrases.size > 7) {
                phrases.minByOrNull {
                    it.lastSpokenDate
                }?.let {
                    deletePhrase(
                        it
                    )
                }
            }
        } else {
            updatePhrase(
                Phrase(
                    recentPhrase.phraseId,
                    PresetCategories.RECENTS.id,
                    phrase.creationDate,
                    Calendar.getInstance().timeInMillis,
                    phrase.localizedUtterance,
                    phrase.sortOrder
                )
            )
        }
    }

    override suspend fun addCategory(category: CategoryDto) {
        database.categoryDao().insertCategory(category)
    }

    suspend fun populateCategories(categories: List<CategoryDto>) {
        database.categoryDao().insertCategories(*categories.toTypedArray())
    }

    suspend fun populatePhrases(phrases: List<Phrase>) {
        database.phraseDao().insertPhrases(*phrases.toTypedArray())
    }

    override suspend fun deletePhrase(phrase: Phrase) {
        database.phraseDao().deletePhrase(phrase)
    }

    suspend fun deletePhrases(phrases: List<Phrase>) {
        database.phraseDao().deletePhrases(*phrases.toTypedArray())
    }

    suspend fun deleteCategory(category: CategoryDto) {
        database.categoryDao().deleteCategory(category)
    }

    suspend fun updatePhrase(phrase: Phrase) {
        database.phraseDao().updatePhrase(phrase)
    }

    override suspend fun updateCategory(category: CategoryDto) {
        database.categoryDao().updateCategory(category)
    }

    override suspend fun updateCategories(categories: List<CategoryDto>) {
        database.categoryDao().updateCategories(*categories.toTypedArray())
    }

    suspend fun getCategoryById(categoryId: String): CategoryDto {
        return database.categoryDao().getCategoryById(categoryId)
    }

    //Initial DB populate
    suspend fun populateDatabase() {
        PresetCategories.values().forEach { presetCategory ->
            if (presetCategory != PresetCategories.RECENTS && presetCategory != PresetCategories.MY_SAYINGS) {
                val phrasesIds =
                    get<Context>().resources.obtainTypedArray(presetCategory.getArrayId())
                val phraseObjects = mutableListOf<Phrase>()
                for (index in 0 until phrasesIds.length()) {
                    phraseObjects.add(
                        Phrase(
                            0L,
                            presetCategory.id,
                            System.currentTimeMillis(),
                            System.currentTimeMillis(),
                            mapOf(Pair(Locale.getDefault().toString(), context.getString(phrasesIds.getResourceId(index, -1)))),
                            phraseObjects.size
                        )
                    )
                }
                phrasesIds.recycle()
                populatePhrases(phraseObjects)
            }
            database.categoryDao().insertCategories(
                CategoryDto(
                    presetCategory.id,
                    System.currentTimeMillis(),
                    presetCategory.getNameId(),
                    null,
                    false,
                    presetCategory.initialSortOrder
                )

            )
        }
    }
}

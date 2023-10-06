package com.willowtree.vocable.presets

import android.content.Context
import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.room.PhraseLocalizedUtterance
import com.willowtree.vocable.room.PhraseSpokenDate
import com.willowtree.vocable.room.VocableDatabase
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.Locale

class PresetsRepository(val context: Context) : KoinComponent, IPresetsRepository {

    private val database = VocableDatabase.getVocableDatabase(context)

    override suspend fun getAllCategories(): List<CategoryDto> {
        return database.categoryDao().getAllCategories()
    }

    override fun getAllCategoriesFlow(): Flow<List<CategoryDto>> {
        return database.categoryDao().getAllCategoriesFlow()
    }

    override suspend fun getPhrasesForCategory(categoryId: String): List<PhraseDto> {
        return database.categoryDao().getCategoryWithPhrases(categoryId)?.phrases ?: listOf()
    }

    override suspend fun getRecentPhrases(): List<PhraseDto> = database.phraseDao().getRecentPhrases()

    override suspend fun addPhrase(phrase: PhraseDto) {
        database.phraseDao().insertPhrase(phrase)
    }

    override suspend fun addCategory(category: CategoryDto) {
        database.categoryDao().insertCategory(category)
    }

    private suspend fun populatePhrases(phrases: List<PhraseDto>) {
        database.phraseDao().insertPhrases(*phrases.toTypedArray())
    }

    override suspend fun deletePhrase(phraseId: Long) {
        database.phraseDao().deletePhrase(phraseId)
    }

    suspend fun deletePhrases(phrases: List<PhraseDto>) {
        database.phraseDao().deletePhrases(*phrases.toTypedArray())
    }

    override suspend fun deleteCategory(categoryId: String) {
        database.categoryDao().deleteCategory(categoryId)
    }

    override suspend fun updatePhrase(phraseId: Long, localizedUtterance: Map<String, String>) {
        database.phraseDao().updatePhraseLocalizedUtterance(PhraseLocalizedUtterance(phraseId, localizedUtterance))
    }

    override suspend fun updatePhraseLastSpoken(phraseId: Long, lastSpokenDate: Long) {
        database.phraseDao().updatePhraseSpokenDate(PhraseSpokenDate(phraseId, lastSpokenDate))
    }

    override suspend fun updateCategory(category: CategoryDto) {
        database.categoryDao().updateCategory(category)
    }

    override suspend fun updateCategories(categories: List<CategoryDto>) {
        database.categoryDao().updateCategories(*categories.toTypedArray())
    }

    override suspend fun getCategoryById(categoryId: String): CategoryDto {
        return database.categoryDao().getCategoryById(categoryId)
    }

    //Initial DB populate
    suspend fun populateDatabase() {
        PresetCategories.values().forEach { presetCategory ->
            if (presetCategory != PresetCategories.RECENTS && presetCategory != PresetCategories.MY_SAYINGS) {
                val phrasesIds =
                    get<Context>().resources.obtainTypedArray(presetCategory.getArrayId())
                val phraseObjects = mutableListOf<PhraseDto>()
                for (index in 0 until phrasesIds.length()) {
                    phraseObjects.add(
                        PhraseDto(
                            0L,
                            presetCategory.id,
                            System.currentTimeMillis(),
                            null,
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

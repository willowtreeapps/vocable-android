package com.willowtree.vocable.presets

import android.content.Context
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.CategoryPhraseCrossRef
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.room.VocableDatabase
import org.koin.core.KoinComponent
import org.koin.core.get
import java.util.*

class PresetsRepository(context: Context) : KoinComponent {

    private val database = VocableDatabase.getVocableDatabase(context)

    suspend fun getAllCategories(): List<Category> {
        return database.categoryDao().getAllCategories()
    }

    suspend fun getUserGeneratedCategories(): List<Category> {
        return database.categoryDao().getUserGeneratedCategories()
    }

    suspend fun getPhrasesForCategory(categoryId: String): List<Phrase> {
        return database.categoryDao().getCategoryWithPhrases(categoryId).phrases
    }

    suspend fun addPhrase(phrase: Phrase) {
        database.phraseDao().insertPhrase(phrase)
    }

    suspend fun addCategory(category: Category) {
        database.categoryDao().insertCategory(category)
    }

    suspend fun populateCategories(categories: List<Category>) {
        database.categoryDao().insertCategories(*categories.toTypedArray())
    }

    suspend fun populatePhrases(phrases: List<Phrase>) {
        database.phraseDao().insertPhrases(*phrases.toTypedArray())
    }

    suspend fun populateCrossRefs(crossRefs: List<CategoryPhraseCrossRef>) {
        database.categoryPhraseCrossRefDao()
            .insertCategoryPhraseCrossRefs(*crossRefs.toTypedArray())
    }

    suspend fun addCrossRef(crossRef: CategoryPhraseCrossRef) {
        database.categoryPhraseCrossRefDao().insertCategoryPhraseCrossRef(crossRef)
    }

    suspend fun deleteCrossRef(crossRef: CategoryPhraseCrossRef) {
        database.categoryPhraseCrossRefDao().deleteCategoryPhraseCrossRef(crossRef)
    }

    suspend fun deleteCrossRefs(crossRefs: List<CategoryPhraseCrossRef>) {
        database.categoryPhraseCrossRefDao()
            .deleteCategoryPhraseCrossRefs(*crossRefs.toTypedArray())
    }

    suspend fun getCrossRefsForPhraseIds(phraseIds: List<String>): List<CategoryPhraseCrossRef> {
        return database.categoryPhraseCrossRefDao()
            .getCategoryPhraseCrossRefsForPhraseIds(phraseIds)
    }

    suspend fun getCrossRefsForCategoryId(categoryId: String): List<CategoryPhraseCrossRef> {
        return database.categoryPhraseCrossRefDao().getCategoryPhraseCrossRefsForCategoryId(categoryId)
    }

    suspend fun getPhraseById(id: String): Phrase {
        return database.phraseDao().getPhraseById(id)
    }

    suspend fun deletePhrase(phrase: Phrase) {
        database.phraseDao().deletePhrase(phrase)
    }

    suspend fun deletePhrases(phrases: List<Phrase>) {
        database.phraseDao().deletePhrases(*phrases.toTypedArray())
    }

    suspend fun deleteCategory(category: Category) {
        database.categoryDao().deleteCategory(category)
    }

    suspend fun updatePhrase(phrase: Phrase) {
        database.phraseDao().updatePhrase(phrase)
    }

    suspend fun updateCategory(category: Category) {
        database.categoryDao().updateCategory(category)
    }

    suspend fun updateCategories(categories: List<Category>) {
        database.categoryDao().updateCategories(*categories.toTypedArray())
    }

    suspend fun getCategoryById(categoryId: String): Category {
        return database.categoryDao().getCategoryById(categoryId)
    }

    suspend fun populateDatabase() {
        val categories = getAllCategories()
        if (categories.size > 1) {
            return
        }

        val categoryObjects = mutableListOf<Category>()
        val phraseObjects = mutableListOf<Phrase>()
        val crossRefObjects = mutableListOf<CategoryPhraseCrossRef>()


        PresetCategories.values().forEach {
            categoryObjects.add(
                Category(
                    it.id,
                    System.currentTimeMillis(),
                    false,
                    it.getNameId(),
                    null,
                    false,
                    it.initialSortOrder
                )
            )

            if (it.getArrayId() == -1) {
                return@forEach
            }
            val phraseStringIds = get<Context>().resources.obtainTypedArray(it.getArrayId())
            for (index in 0 until phraseStringIds.length()) {
                val phraseId = UUID.randomUUID().toString()
                phraseObjects.add(
                    Phrase(
                        phraseId,
                        System.currentTimeMillis(),
                        false,
                        System.currentTimeMillis(),
                        phraseStringIds.getResourceId(index, -1),
                        null,
                        phraseObjects.size
                    )
                )
                crossRefObjects.add(CategoryPhraseCrossRef(it.id, phraseId))
            }
            phraseStringIds.recycle()
        }

        populateCategories(categoryObjects)
        populatePhrases(phraseObjects)
        populateCrossRefs(crossRefObjects)
    }

}
package com.willowtree.vocable.presets

import android.content.Context
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.CategoryPhraseCrossRef
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.room.VocableDatabase
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import java.util.*

class PresetsRepository : KoinComponent {

    private val database: VocableDatabase by inject()

    suspend fun getAllCategories(): List<Category> {
        return database.categoryDao().getAllCategories()
    }

    suspend fun getUserGeneratedCategories(): List<Category> {
        return database.categoryDao().getUserGeneratedCategories()
    }

    suspend fun getPhrasesForCategory(categoryId: String): List<Phrase> {
        return database.categoryDao().getCategoryWithPhrases(categoryId)?.phrases ?: emptyList()
    }

    suspend fun addPhrase(phrase: Phrase) {
        database.phraseDao().insertPhrase(phrase)
    }

    suspend fun addPhraseToRecents(phrase: Phrase) {
        val category = database.categoryDao().getCategoryById(PresetCategories.RECENTS.id)

        // get the cross ref for the Recents category and the given phrase
        var categoryPhraseCrossRef = database.categoryPhraseCrossRefDao()
            .getSpecificCategoryPhraseCrossRef(category.categoryId, phrase.phraseId)

        if (categoryPhraseCrossRef == null) {
            // if it doesn't exist in the db, we need to create it and insert it
            categoryPhraseCrossRef = CategoryPhraseCrossRef(
                category.categoryId,
                phrase.phraseId,
                System.currentTimeMillis()
            )
            database.categoryPhraseCrossRefDao()
                .insertCategoryPhraseCrossRef(categoryPhraseCrossRef)

            // get a list of all the cross refs listed under "Recents"
            val recentsCategoryPhraseCrossRefs = database.categoryPhraseCrossRefDao()
                .getCategoryPhraseCrossRefsForCategoryId(PresetCategories.RECENTS.id)
            if (recentsCategoryPhraseCrossRefs.size > 9) {
                // if there's more than 9, we need to delete the oldest one
                val oldestCrossRef = recentsCategoryPhraseCrossRefs.sortedBy { it.timestamp }[0]
                database.categoryPhraseCrossRefDao().deleteCategoryPhraseCrossRef(oldestCrossRef)
            }
        } else {
            // if it does exist, we just need to update the timestamp
            database.categoryPhraseCrossRefDao().updateCrossRefTimestamp(
                category.categoryId,
                phrase.phraseId,
                System.currentTimeMillis()
            )
        }
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

    suspend fun deleteCrossRefsForCategoryIds(categoryIds: List<String>) {
        database.categoryPhraseCrossRefDao().deleteCategoryPhraseCrossRefsForPhraseIds(categoryIds)
    }

    suspend fun getCrossRefsForCategoryId(categoryId: String): List<CategoryPhraseCrossRef> {
        return database.categoryPhraseCrossRefDao()
            .getCategoryPhraseCrossRefsForCategoryId(categoryId)
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

    suspend fun deleteNonUserGeneratedPhrases() {
        database.phraseDao().deleteNonUserGeneratedPhrases()
    }

    suspend fun updateCategories(categories: List<Category>) {
        database.categoryDao().updateCategories(*categories.toTypedArray())
    }

    suspend fun getCategoryById(categoryId: String): Category {
        return database.categoryDao().getCategoryById(categoryId)
    }

    suspend fun getNumberOfShownCategories(): Int {
        return database.categoryDao().getNumberOfShownCategories()
    }

    suspend fun populateDatabase() {
        val categoryObjects = mutableListOf<Category>()
        val phraseObjects = mutableListOf<Phrase>()
        val crossRefObjects = mutableListOf<CategoryPhraseCrossRef>()


        PresetCategories.values().forEach {
            val existingCategory = getCategoryById(it.id)
            categoryObjects.add(
                if (existingCategory != null) {
                    Category(
                        it.id,
                        System.currentTimeMillis(),
                        false,
                        it.getNameId(),
                        null,
                        existingCategory?.hidden,
                        existingCategory?.sortOrder
                    )
                } else {
                    Category(
                        it.id,
                        System.currentTimeMillis(),
                        false,
                        it.getNameId(),
                        null,
                        false,
                        it.initialSortOrder
                    )
                }

            )

            // delete non-user-generated cross-refs
            val nonUserCategoryIds = PresetCategories.values().map { category -> category.id }
            deleteCrossRefsForCategoryIds(nonUserCategoryIds)

            if (it.getArrayId() == -1) {
                return@forEach
            }

            // delete non-user-generated phrases
            deleteNonUserGeneratedPhrases()

            // re-add them
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

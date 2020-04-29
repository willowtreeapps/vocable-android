package com.willowtree.vocable.presets

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.willowtree.vocable.R
import com.willowtree.vocable.room.*
import com.willowtree.vocable.room.models.PresetsObject
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import java.nio.charset.Charset
import java.util.*

class PresetsRepository(context: Context) : KoinComponent {

    private val database = VocableDatabase.getVocableDatabase(context)
    private val sharedPrefs: VocableSharedPreferences by inject()
    private val moshi: Moshi by inject()

    suspend fun getAllCategories(): List<Category> {
        return database.categoryDao().getAllCategories()
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
        database.categoryPhraseCrossRefDao().deleteCategoryPhraseCrossRefDao(crossRef)
    }

    suspend fun deletePhrase(phrase: Phrase) {
        database.phraseDao().deletePhrase(phrase)
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

    suspend fun populateDatabase(numbersCategoryId: String, mySayingsCategoryId: String) {
        val categories = getAllCategories()
        if (categories.isNotEmpty()) {
            return
        }
        val presets = withContext(Dispatchers.IO) {
            var json = ""
            try {
                val inputStream = get<Context>().assets.open("json/presets.json")
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                json = String(buffer, Charset.forName("UTF-8"))
            } catch (e: Exception) {
                Log.e("populateDatabase", e.message ?: "Error reading JSON")
            }

            var presetsObject: PresetsObject? = null
            try {
                presetsObject = moshi.adapter(PresetsObject::class.java).fromJson(json)
            } catch (e: Exception) {
                Log.e("populateDatabase", e.message ?: "Error parsing JSON")
            }
            return@withContext presetsObject
        }

        val categoryObjects = mutableListOf<Category>()
        val phraseObjects = mutableListOf<Phrase>()
        val crossRefObjects = mutableListOf<CategoryPhraseCrossRef>()

        // Populate the presets from JSON
        presets?.categories?.forEach {
            categoryObjects.add(
                Category(
                    it.id,
                    System.currentTimeMillis(),
                    false,
                    it.localizedName,
                    it.hidden,
                    categoryObjects.size
                )
            )
        }

        presets?.phrases?.forEach { presetPhrase ->
            phraseObjects.add(
                Phrase(
                    presetPhrase.id,
                    System.currentTimeMillis(),
                    false,
                    System.currentTimeMillis(),
                    presetPhrase.localizedUtterance,
                    phraseObjects.size
                )
            )
            presetPhrase.categoryIds.forEach { categoryId ->
                crossRefObjects.add(CategoryPhraseCrossRef(categoryId, presetPhrase.id))
            }
        }

        // Populate the numbers category from arrays.xml
        get<Context>().resources.getStringArray(R.array.category_123).forEach {
            val phraseId = UUID.randomUUID().toString()
            phraseObjects.add(
                Phrase(
                    phraseId,
                    System.currentTimeMillis(),
                    false,
                    System.currentTimeMillis(),
                    mapOf(Pair(Locale.US.language, it)),
                    phraseObjects.size
                )
            )
            crossRefObjects.add(CategoryPhraseCrossRef(numbersCategoryId, phraseId))
        }

        // Create My Sayings category
        val mySayingsCategory =
            categoryObjects.first { it.categoryId == mySayingsCategoryId }
        val mySayings = sharedPrefs.getMySayings()
        mySayings.forEach {
            val phraseId = UUID.randomUUID().toString()
            phraseObjects.add(
                Phrase(
                    phraseId,
                    System.currentTimeMillis(),
                    true,
                    System.currentTimeMillis(),
                    mapOf(Pair(Locale.US.language, it)),
                    phraseObjects.size
                )
            )
            crossRefObjects.add(
                CategoryPhraseCrossRef(
                    mySayingsCategory.categoryId,
                    phraseId
                )
            )
            sharedPrefs.setMySayings(emptySet())
        }

        populateCategories(categoryObjects)
        populatePhrases(phraseObjects)
        populateCrossRefs(crossRefObjects)
    }
}
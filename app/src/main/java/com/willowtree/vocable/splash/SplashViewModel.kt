package com.willowtree.vocable.splash

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.Moshi
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.R
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.CategoryPhraseCrossRef
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.room.PresetsObject
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.get
import org.koin.core.inject
import java.nio.charset.Charset
import java.util.*

class SplashViewModel : BaseViewModel() {

    private val presetsRepository: PresetsRepository by inject()
    private val moshi: Moshi by inject()
    private val sharedPrefs: VocableSharedPreferences by inject()

    private val liveExitSplash = MutableLiveData<Boolean>()
    val exitSplash: LiveData<Boolean> = liveExitSplash

    init {
        populateDatabase()
    }

    private fun populateDatabase() {
        backgroundScope.launch {
            val categories = presetsRepository.getAllCategories()
            if (categories.isNotEmpty()) {
                liveExitSplash.postValue(true)
                return@launch
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
            val numbersCategoryTitle = get<Context>().getString(R.string.category_123)
            val numbersCategory = Category(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                false,
                mapOf(Pair(Locale.US.language, numbersCategoryTitle)),
                false,
                categoryObjects.size
            )
            sharedPrefs.setNumbersCategoryId(numbersCategory.categoryId)
            categoryObjects.add(numbersCategory)
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
                crossRefObjects.add(CategoryPhraseCrossRef(numbersCategory.categoryId, phraseId))
            }

            // Create My Sayings category
            val mySayingsTitle = get<Context>().getString(R.string.category_my_sayings)
            var mySayingsCategory = Category(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                false,
                mapOf(Pair(Locale.US.language, mySayingsTitle)),
                true,
                categoryObjects.size
            )
            sharedPrefs.setMySayingsCategoryId(mySayingsCategory.categoryId)

            val mySayings = sharedPrefs.getMySayings()
            if (mySayings.isNotEmpty()) {
                mySayingsCategory = mySayingsCategory.apply {
                    hidden = false
                }
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
                }
            }
            categoryObjects.add(mySayingsCategory)

            with(presetsRepository) {
                populateCategories(categoryObjects)
                populatePhrases(phraseObjects)
                populateCrossRefs(crossRefObjects)
            }

            liveExitSplash.postValue(true)
        }
    }
}
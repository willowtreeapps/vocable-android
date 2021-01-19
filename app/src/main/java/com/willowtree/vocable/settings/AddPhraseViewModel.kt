package com.willowtree.vocable.settings

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.CategoryPhraseCrossRef
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.inject
import java.util.*

class AddPhraseViewModel : BaseViewModel() {

    companion object {
        private const val PHRASE_UPDATED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    init {
        populateMySayings()
    }

    private fun populateMySayings() {

    }

    fun deletePhrase(phrase: Phrase) {
        backgroundScope.launch {
            with(presetsRepository) {
                deletePhrase(phrase)
                val mySayingsCategory = getCategoryById(PresetCategories.USER_FAVORITES.id)

                // Should handle this better, I will ask for assistance
                if (mySayingsCategory == null) {
                    Log.e("Error", "My Sayings Category from database is null")
                    return@launch
                }

                deleteCrossRef(
                    CategoryPhraseCrossRef(
                        mySayingsCategory.categoryId,
                        phrase.phraseId
                    )
                )
            }
            populateMySayings()
        }
    }

    fun updatePhrase(phrase: Phrase) {
        backgroundScope.launch {
            presetsRepository.updatePhrase(phrase)
            populateMySayings()

            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_UPDATED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }

    fun addNewPhrase(phraseStr: String, categoryId: String) {
        backgroundScope.launch {
            val mySayingsPhrases = presetsRepository.getPhrasesForCategory(categoryId)
            val phraseId = UUID.randomUUID().toString()
            presetsRepository.addPhrase(
                Phrase(
                    phraseId,
                    System.currentTimeMillis(),
                    true,
                    System.currentTimeMillis(),
                    null,
                    mapOf(Pair(Locale.getDefault().toString(), phraseStr)),
                    mySayingsPhrases.size
                )
            )
            presetsRepository.addCrossRef(CategoryPhraseCrossRef(categoryId, phraseId))

            populateMySayings()

            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_UPDATED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }

}

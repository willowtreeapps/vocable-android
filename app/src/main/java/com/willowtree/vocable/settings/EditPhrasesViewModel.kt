package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.CategoryPhraseCrossRef
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.inject
import java.util.*

class EditPhrasesViewModel : BaseViewModel() {

    companion object {
        private const val PHRASE_UPDATED_DELAY = 2000L
        private const val PHRASE_ADDED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()
    private val sharedPrefs: VocableSharedPreferences by inject()

    private val liveMySayingsList = MutableLiveData<List<Phrase>>()
    val mySayingsList: LiveData<List<Phrase>> = liveMySayingsList

    private val liveSetButtonsEnabled = MutableLiveData<Boolean>()
    val setButtonEnabled: LiveData<Boolean> = liveSetButtonsEnabled

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    init {
        populateMySayings()
    }

    private fun populateMySayings() {
        backgroundScope.launch {

            val phrases =
                presetsRepository.getPhrasesForCategory(sharedPrefs.getMySayingsCategoryId())

            liveMySayingsList.postValue(phrases)
        }
    }

    fun deletePhrase(phrase: Phrase) {
        backgroundScope.launch {
            with(presetsRepository) {
                deletePhrase(phrase)
                val mySayingsCategory = getCategoryById(sharedPrefs.getMySayingsCategoryId())
                deleteCrossRef(
                    CategoryPhraseCrossRef(
                        mySayingsCategory.categoryId,
                        phrase.phraseId
                    )
                )
                val catPhraseList = getPhrasesForCategory(mySayingsCategory.categoryId)
                if (catPhraseList.isEmpty()) {
                    updateCategory(mySayingsCategory.apply {
                        hidden = true
                    })
                }
            }
            populateMySayings()
        }
    }

    fun setEditButtonsEnabled(enabled: Boolean) {
        liveSetButtonsEnabled.postValue(enabled)
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

    fun addNewPhrase(phraseStr: String) {
        backgroundScope.launch {
            val categoryId = sharedPrefs.getMySayingsCategoryId()
            val mySayingsPhrases = presetsRepository.getPhrasesForCategory(categoryId)
            val phraseId = UUID.randomUUID().toString()
            // TODO: Use currently set Locale
            presetsRepository.addPhrase(
                Phrase(
                    phraseId,
                    System.currentTimeMillis(),
                    true,
                    System.currentTimeMillis(),
                    mapOf(Pair(Locale.US.language, phraseStr)),
                    mySayingsPhrases.size
                )
            )
            presetsRepository.addCrossRef(CategoryPhraseCrossRef(categoryId, phraseId))
            val mySayingsCategory =
                presetsRepository.getCategoryById(sharedPrefs.getMySayingsCategoryId())
            if (mySayingsCategory.hidden) {
                presetsRepository.updateCategory(mySayingsCategory.apply {
                    hidden = false
                })
            }

            populateMySayings()

            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_ADDED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }

}

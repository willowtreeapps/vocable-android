package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.CategoryPhraseCrossRef
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.inject
import java.util.*

class EditPhrasesViewModel(numbersCategoryId: String, mySayingsCategoryId: String) :
    BaseViewModel(numbersCategoryId, mySayingsCategoryId) {

    companion object {
        private const val PHRASE_UPDATED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

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
                presetsRepository.getPhrasesForCategory(mySayingsCategoryId).sortedBy { it.sortOrder }

            liveMySayingsList.postValue(phrases)
        }
    }

    fun deletePhrase(phrase: Phrase) {
        backgroundScope.launch {
            with(presetsRepository) {
                deletePhrase(phrase)
                val mySayingsCategory = getCategoryById(mySayingsCategoryId)
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
            val mySayingsPhrases = presetsRepository.getPhrasesForCategory(mySayingsCategoryId)
            val phraseId = UUID.randomUUID().toString()
            presetsRepository.addPhrase(
                Phrase(
                    phraseId,
                    System.currentTimeMillis(),
                    true,
                    System.currentTimeMillis(),
                    mapOf(Pair(Locale.getDefault().toString(), phraseStr)),
                    mySayingsPhrases.size
                )
            )
            presetsRepository.addCrossRef(CategoryPhraseCrossRef(mySayingsCategoryId, phraseId))

            populateMySayings()

            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_UPDATED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }

}

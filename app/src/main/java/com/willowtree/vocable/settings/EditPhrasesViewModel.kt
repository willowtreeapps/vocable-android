package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.keyboard.KeyboardViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.inject

class EditPhrasesViewModel: BaseViewModel() {

    companion object {
        private const val PHRASE_UPDATED_DELAY = 2000L
        private const val PHRASE_ADDED_DELAY = 2000L
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

            val phrases = presetsRepository.getPhrasesForCategory(presetsRepository.getMySayingsId())

            liveMySayingsList.postValue(phrases)
        }
    }

    fun deletePhrase(phrase: Phrase) {
        backgroundScope.launch {
            presetsRepository.deletePhrase(phrase)
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
            val categoryId = presetsRepository.getMySayingsId()
            presetsRepository.addPhrase(
                Phrase(
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    true,
                    0L,
                    phraseStr,
                    categoryId
                )
            )

            populateMySayings()

            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_ADDED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }
  
}

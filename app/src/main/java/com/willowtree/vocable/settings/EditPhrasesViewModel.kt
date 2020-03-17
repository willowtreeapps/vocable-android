package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.inject

class EditPhrasesViewModel: BaseViewModel() {

    companion object {
        private const val PHRASE_UPDATED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

    private val liveMySayingsList = MutableLiveData<List<Phrase>>()
    val mySayingsList: LiveData<List<Phrase>> = liveMySayingsList

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

    fun updatePhrase(phrase: Phrase) {
        backgroundScope.launch {
            presetsRepository.updatePhrase(phrase)
            populateMySayings()

            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_UPDATED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }
}

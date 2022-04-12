package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.util.*

class EditPhrasesViewModel : BaseViewModel() {

    companion object {
        private const val PHRASE_UPDATED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    fun updatePhrase(phrase: Phrase) {
        backgroundScope.launch {
            presetsRepository.updatePhrase(phrase)

            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_UPDATED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }

    fun addNewPhrase(phraseStr: String) {
        backgroundScope.launch {
            val mySayingsPhrases = presetsRepository.getPhrasesForCategory(PresetCategories.MY_SAYINGS.id)
            presetsRepository.addPhrase(
                Phrase(
                    0L,
                    PresetCategories.RECENTS.id,
                    System.currentTimeMillis(),
                    true,
                    System.currentTimeMillis(),
                    null,
                    mapOf(Pair(Locale.getDefault().toString(), phraseStr)),
                    mySayingsPhrases.size
                )
            )

            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_UPDATED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }
}

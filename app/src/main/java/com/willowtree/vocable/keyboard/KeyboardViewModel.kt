package com.willowtree.vocable.keyboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.inject

class KeyboardViewModel : BaseViewModel() {

    private val presetsRepository: PresetsRepository by inject()

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    fun addNewPhrase(phraseStr: String) {
        backgroundScope.launch {
            presetsRepository.addPhrase(
                Phrase(
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    true,
                    0L,
                    phraseStr,
                    -1L
                )
            )
            liveShowPhraseAdded.postValue(true)
            delay(2000)
            liveShowPhraseAdded.postValue(false)
        }
    }
}
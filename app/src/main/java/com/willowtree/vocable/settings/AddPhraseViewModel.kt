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

class AddPhraseViewModel : BaseViewModel() {

    companion object {
        private const val PHRASE_UPDATED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    fun addNewPhrase(phraseStr: String, categoryId: String) {
        backgroundScope.launch {
            val mySayingsPhrases = presetsRepository.getPhrasesForCategory(categoryId)
            presetsRepository.addPhrase(
                Phrase(
                    0L,
                    categoryId,
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

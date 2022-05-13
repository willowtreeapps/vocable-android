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
import timber.log.Timber
import java.util.*

class AddPhraseViewModel : BaseViewModel() {

    companion object {
        private const val PHRASE_UPDATED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    fun addNewPhrase(phraseStr: String, categoryId: String) {
        Timber.d("WILL: log 1")
        backgroundScope.launch {
            Timber.d("WILL: log 2")
            val mySayingsPhrases = presetsRepository.getPhrasesForCategory(categoryId)
            if (mySayingsPhrases.none {
                    it.localizedUtterance?.containsValue(phraseStr) == true
            }) {
                Timber.d("WILL: saving like normal")
                presetsRepository.addPhrase(
                    Phrase(
                        0L,
                        categoryId,
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        mapOf(Pair(Locale.getDefault().toString(), phraseStr)),
                        mySayingsPhrases.size
                    )
                )
                liveShowPhraseAdded.postValue(true)
            } else {
                Timber.d("WILL: not saving")
                liveShowPhraseAdded.postValue(false)
            }
        }
    }
}

package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.PhraseDto
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class AddPhraseViewModel : ViewModel(), KoinComponent {

    private val presetsRepository: PresetsRepository by inject()

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    fun addNewPhrase(phraseStr: String, categoryId: String) {
        viewModelScope.launch {
            val mySayingsPhrases = presetsRepository.getPhrasesForCategory(categoryId)
            if (mySayingsPhrases.none {
                    it.localizedUtterance?.containsValue(phraseStr) == true
            }) {
                presetsRepository.addPhrase(
                    PhraseDto(
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
                liveShowPhraseAdded.postValue(false)
            }
        }
    }
}

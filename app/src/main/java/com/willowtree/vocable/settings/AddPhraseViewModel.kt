package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.PhrasesUseCase
import com.willowtree.vocable.presets.LegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

class AddPhraseViewModel : ViewModel(), KoinComponent {

    private val presetsRepository: LegacyCategoriesAndPhrasesRepository by inject()
    private val phrasesUseCase: PhrasesUseCase by inject()

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    fun addNewPhrase(phraseStr: String, categoryId: String) {
        viewModelScope.launch {
            val mySayingsPhrases = presetsRepository.getPhrasesForCategory(categoryId)
            if (mySayingsPhrases.none {
                    it.localizedUtterance?.containsValue(phraseStr) == true
                }) {
                phrasesUseCase.addPhrase(
                    LocalesWithText(mapOf(Pair(Locale.getDefault().toString(), phraseStr))),
                    categoryId
                )
                liveShowPhraseAdded.postValue(true)
            } else {
                liveShowPhraseAdded.postValue(false)
            }
        }
    }
}

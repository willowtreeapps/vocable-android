package com.willowtree.vocable.keyboard

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

class KeyboardViewModel(numbersCategoryId: String, mySayingsCategoryId: String) :
    BaseViewModel(numbersCategoryId, mySayingsCategoryId) {

    companion object {
        private const val PHRASE_ADDED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    fun addNewPhrase(phraseStr: String) {
        backgroundScope.launch {
            val mySayingsCategory =
                presetsRepository.getCategoryById(mySayingsCategoryId)
            val phraseId = UUID.randomUUID().toString()
            val mySayingsPhrases =
                presetsRepository.getPhrasesForCategory(mySayingsCategoryId)
            with(presetsRepository) {
                addPhrase(
                    Phrase(
                        phraseId,
                        System.currentTimeMillis(),
                        true,
                        System.currentTimeMillis(),
                        mapOf(Pair(Locale.getDefault().toString(), phraseStr)),
                        mySayingsPhrases.size
                    )
                )
                addCrossRef(
                    CategoryPhraseCrossRef(
                        mySayingsCategory.categoryId,
                        phraseId
                    )
                )
                if (mySayingsCategory.hidden) {
                    updateCategory(mySayingsCategory.apply {
                        hidden = false
                    })
                }
            }
            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_ADDED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }
}
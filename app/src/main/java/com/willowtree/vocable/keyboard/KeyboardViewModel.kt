package com.willowtree.vocable.keyboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.CategoryPhraseCrossRef
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.inject
import java.util.*

class KeyboardViewModel : BaseViewModel() {

    companion object {
        private const val PHRASE_ADDED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    fun addNewPhrase(phraseStr: String) {
        backgroundScope.launch {
            val mySayingsCategory =
                presetsRepository.getCategoryById(PresetCategories.USER_FAVORITES.id)
            val phraseId = UUID.randomUUID().toString()
            val mySayingsPhrases =
                presetsRepository.getPhrasesForCategory(PresetCategories.USER_FAVORITES.id)
            with(presetsRepository) {
                addPhrase(
                    Phrase(
                        phraseId,
                        System.currentTimeMillis(),
                        true,
                        System.currentTimeMillis(),
                        null,
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
            }
            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_ADDED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }
}
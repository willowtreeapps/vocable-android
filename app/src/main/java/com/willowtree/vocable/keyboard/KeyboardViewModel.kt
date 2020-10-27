package com.willowtree.vocable.keyboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.CategoryPhraseCrossRef
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.utils.LocalizedResourceUtility
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.inject
import java.util.*

class KeyboardViewModel(private val presetsRepository: PresetsRepository, private val localizedResourceUtility: LocalizedResourceUtility) : BaseViewModel() {

    companion object {
        const val PHRASE_ADDED_DELAY = 2000L
    }

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    private val liveIsPhraseSaved = MutableLiveData<Boolean>()
    val isPhraseSaved: LiveData<Boolean> = liveIsPhraseSaved

    var currentText = ""
        set(value) {
            field = value
            checkIfPhraseSaved()
        }

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

            checkIfPhraseSaved()

            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_ADDED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }

    private fun checkIfPhraseSaved() {
        backgroundScope.launch {
            val mySayingsPhrases = presetsRepository.getPhrasesForCategory(PresetCategories.USER_FAVORITES.id)
            val isSaved = mySayingsPhrases.map { localizedResourceUtility.getTextFromPhrase(it) }.contains(currentText)
            liveIsPhraseSaved.postValue(isSaved)
        }
    }
}
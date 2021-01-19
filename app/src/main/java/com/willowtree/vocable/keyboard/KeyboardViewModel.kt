package com.willowtree.vocable.keyboard

import android.util.Log
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

class KeyboardViewModel : BaseViewModel() {

    companion object {
        const val PHRASE_ADDED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()
    private val localizedResourceUtility: LocalizedResourceUtility by inject()

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

            // Should handle this better, I will ask for assistance
            if (mySayingsCategory == null) {
                Log.e("Error", "My Sayings Category from database is null")
                return@launch
            }

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
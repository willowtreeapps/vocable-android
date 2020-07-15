package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.CategoryPhraseCrossRef
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.inject
import java.util.*

class EditPhrasesViewModel(private val category: Category) : BaseViewModel() {

    companion object {
        private const val PHRASE_UPDATED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

    private val livePhrasesList = MutableLiveData<List<Phrase>>()
    val phrasesList: LiveData<List<Phrase>> = livePhrasesList

    private val liveSetButtonsEnabled = MutableLiveData<Boolean>()
    val setButtonEnabled: LiveData<Boolean> = liveSetButtonsEnabled

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    init {
        populatePhrases()
    }

    private fun populatePhrases() {
        backgroundScope.launch {
            val phrases = presetsRepository.getPhrasesForCategory(category.categoryId).sortedBy { it.sortOrder }

            livePhrasesList.postValue(phrases)
        }
    }

    fun deletePhrase(phrase: Phrase) {
        backgroundScope.launch {
            with(presetsRepository) {
                deletePhrase(phrase)
                deleteCrossRef(
                    CategoryPhraseCrossRef(
                        category.categoryId,
                        phrase.phraseId
                    )
                )
            }
            populatePhrases()
        }
    }

    fun setEditButtonsEnabled(enabled: Boolean) {
        liveSetButtonsEnabled.postValue(enabled)
    }

    fun updatePhrase(phrase: Phrase) {
        backgroundScope.launch {
            presetsRepository.updatePhrase(phrase)
            populatePhrases()

            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_UPDATED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }

    fun addNewPhrase(phraseStr: String) {
        backgroundScope.launch {
            val phrases = presetsRepository.getPhrasesForCategory(category.categoryId)
            val phraseId = UUID.randomUUID().toString()
            presetsRepository.addPhrase(
                Phrase(
                    phraseId,
                    System.currentTimeMillis(),
                    true,
                    System.currentTimeMillis(),
                    null,
                    mapOf(Pair(Locale.getDefault().toString(), phraseStr)),
                    phrases.size
                )
            )

            presetsRepository.addCrossRef(CategoryPhraseCrossRef(category.categoryId, phraseId))

            populatePhrases()

            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_UPDATED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }

}

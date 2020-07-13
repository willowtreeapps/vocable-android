package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.CategoryPhraseCrossRef
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.inject
import java.util.*

class EditPhrasesViewModel : BaseViewModel() {

    companion object {
        private const val PHRASE_UPDATED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

    private val liveCategory = MutableLiveData<Category>()
    val category: LiveData<Category> = liveCategory

    private val livePhrasesList = MutableLiveData<List<Phrase>>()
    val phrasesList: LiveData<List<Phrase>> = livePhrasesList

    private val liveSetButtonsEnabled = MutableLiveData<Boolean>()
    val setButtonEnabled: LiveData<Boolean> = liveSetButtonsEnabled

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    init {
        populateMySayings()
    }

    private fun populateMySayings() {
        backgroundScope.launch {
            val phrases = category.value?.categoryId?.let { categoryId ->
                presetsRepository.getPhrasesForCategory(categoryId).sortedBy { it.sortOrder }
            }

            livePhrasesList.postValue(phrases)
        }
    }

    fun setCategory(category: Category) {
        backgroundScope.launch {
            liveCategory.postValue(category)
            populateMySayings()
        }
    }

    fun deletePhrase(phrase: Phrase) {
        backgroundScope.launch {
            with(presetsRepository) {
                deletePhrase(phrase)
                val mySayingsCategory = getCategoryById(PresetCategories.USER_FAVORITES.id)
                deleteCrossRef(
                    CategoryPhraseCrossRef(
                        mySayingsCategory.categoryId,
                        phrase.phraseId
                    )
                )
            }
            populateMySayings()
        }
    }

    fun setEditButtonsEnabled(enabled: Boolean) {
        liveSetButtonsEnabled.postValue(enabled)
    }

    fun updatePhrase(phrase: Phrase) {
        backgroundScope.launch {
            presetsRepository.updatePhrase(phrase)
            populateMySayings()

            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_UPDATED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }

    fun addNewPhrase(phraseStr: String) {
        backgroundScope.launch {
            val mySayingsPhrases = presetsRepository.getPhrasesForCategory(PresetCategories.USER_FAVORITES.id)
            val phraseId = UUID.randomUUID().toString()
            presetsRepository.addPhrase(
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
            presetsRepository.addCrossRef(CategoryPhraseCrossRef(PresetCategories.USER_FAVORITES.id, phraseId))

            populateMySayings()

            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_UPDATED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }

}

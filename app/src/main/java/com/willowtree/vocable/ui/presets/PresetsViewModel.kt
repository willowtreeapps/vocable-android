package com.willowtree.vocable.ui.presets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.domain.usecase.ICategoriesUseCase
import com.willowtree.vocable.domain.usecase.IPhrasesUseCase
import com.willowtree.vocable.domain.model.Phrase
import com.willowtree.vocable.domain.model.PhraseGridItem
import com.willowtree.vocable.domain.model.PresetCategories
import com.willowtree.vocable.core.ILocalizedResourceUtility
import com.willowtree.vocable.core.IVocableSharedPreferences
import com.willowtree.vocable.core.IdlingResourceContainer
import com.willowtree.vocable.core.VocableTextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the [PresetsScreen]. Manages state related to preset categories and phrases,
 * handles user interactions, and listens to changes in categories and phrases from the use cases.
 */
class PresetsViewModel(
    private val categoriesUseCase: ICategoriesUseCase,
    private val phrasesUseCase: IPhrasesUseCase,
    private val idlingResourceContainer: IdlingResourceContainer,
    private val localizedResourceUtility: ILocalizedResourceUtility,
    private val sharedPreferences: IVocableSharedPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(PresetsState())
    val state = _state.asStateFlow()

    private val liveNavToAddPhrase = MutableLiveData<Boolean>()

    init {
        viewModelScope.launch {
            idlingResourceContainer.run {
                val initialCategoryId = categoriesUseCase.categories().first().firstOrNull()?.categoryId
                if (initialCategoryId != null) {
                    _state.update { it.copy(selectedCategory = categoriesUseCase.categories().first().first()) }
                }
            }
        }

        viewModelScope.launch {
            categoriesUseCase.categories()
                .map { categories -> categories.filter { !it.hidden } }
                .collect { categories ->
                    _state.update { it.copy(categories = categories) }
                }
        }

        viewModelScope.launch {
            VocableTextToSpeech.isSpeaking.observeForever { isSpeaking ->
                _state.update { it.copy(isSpeaking = isSpeaking ?: false) }
            }
        }

        viewModelScope.launch {
            _state.map { it.selectedCategory?.categoryId }
                .distinctUntilChanged()
                .filterNotNull()
                .flatMapLatest { selectedId ->
                    phrasesUseCase.getPhrasesForCategoryFlow(selectedId).map { phrases ->
                        Pair(selectedId, mapPhrasesToGridItems(phrases, selectedId))
                    }
                }
                .collect { (selectedId, phrases) ->
                    _state.update { it.copy(currentPhrases = phrases, currentPhrasesCategoryId = selectedId) }
                }
        }
    }

    fun refreshPhrases() {
        val categoryId = _state.value.selectedCategory?.categoryId ?: return
        viewModelScope.launch {
            val phrases = phrasesUseCase.getPhrasesForCategoryFlow(categoryId).first()
            _state.update { it.copy(currentPhrases = mapPhrasesToGridItems(phrases, categoryId)) }
        }
    }

    private fun mapPhrasesToGridItems(phrases: List<Phrase>, categoryId: String): List<PhraseGridItem> {
        val phraseGridItems = phrases.run {
            if (categoryId != PresetCategories.RECENTS.id) sortedBy { it.sortOrder } else this
        }.map {
            PhraseGridItem.Phrase(it.phraseId, localizedResourceUtility.getTextFromPhrase(it))
        }
        return if (categoryId != PresetCategories.RECENTS.id &&
            categoryId != PresetCategories.USER_KEYPAD.id &&
            phrases.isNotEmpty()
        ) {
            phraseGridItems + PhraseGridItem.AddPhrase
        } else {
            phraseGridItems
        }
    }

    fun onIntent(intent: PresetsIntent) {
        when (intent) {
            is PresetsIntent.OnCategorySelected -> {
                viewModelScope.launch {
                    val category = _state.value.categories.find { it.categoryId == intent.categoryId }
                    _state.update { it.copy(selectedCategory = category) }
                }
            }
            is PresetsIntent.AddToRecents -> {
                viewModelScope.launch {
                    idlingResourceContainer.run {
                        phrasesUseCase.updatePhraseLastSpokenTime(intent.phraseId)
                    }
                }
            }
            is PresetsIntent.UpdateActiveText -> {
                _state.update { it.copy(activeText = intent.text) }
            }
            is PresetsIntent.Speak -> {
                _state.update { it.copy(activeText = intent.text) }
                val languageTag = sharedPreferences.getSelectedLanguageTag()
                val locale = if (!languageTag.isNullOrEmpty()) Locale.forLanguageTag(languageTag) else null
                VocableTextToSpeech.speak(
                    locale = locale,
                    text = intent.text,
                    selectedVoiceName = sharedPreferences.getSelectedVoiceName()
                )
                viewModelScope.launch {
                    idlingResourceContainer.run {
                        phrasesUseCase.updatePhraseLastSpokenTime(intent.phraseId)
                    }
                }
            }
            is PresetsIntent.NavToAddPhrase -> {
                liveNavToAddPhrase.value = true
                liveNavToAddPhrase.value = false
            }
        }
    }
}

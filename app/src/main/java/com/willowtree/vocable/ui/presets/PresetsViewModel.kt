package com.willowtree.vocable.ui.presets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.domain.usecase.ICategoriesUseCase
import com.willowtree.vocable.domain.usecase.IPhrasesUseCase
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

        // Fix: Use flatMapLatest instead of nested collect to properly switch flows when category changes
        viewModelScope.launch {
            _state.map { it.selectedCategory?.categoryId }
                .distinctUntilChanged()
                .filterNotNull()
                .flatMapLatest { selectedId ->
                    phrasesUseCase.getPhrasesForCategoryFlow(selectedId).map { phrases ->
                        val phraseGridItems: List<PhraseGridItem> = phrases.run {
                            if (selectedId != PresetCategories.RECENTS.id) {
                                sortedBy { it.sortOrder }
                            } else {
                                this
                            }
                        }.map {
                            PhraseGridItem.Phrase(
                                it.phraseId,
                                localizedResourceUtility.getTextFromPhrase(it)
                            )
                        }
                        
                        val items = if (selectedId != PresetCategories.RECENTS.id && selectedId != PresetCategories.USER_KEYPAD.id && phrases.isNotEmpty()) {
                            phraseGridItems + PhraseGridItem.AddPhrase
                        } else {
                            phraseGridItems
                        }
                        
                        Pair(selectedId, items)
                    }
                }
                .collect { (selectedId, phrases) ->
                    _state.update { it.copy(currentPhrases = phrases, currentPhrasesCategoryId = selectedId) }
                }
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
                VocableTextToSpeech.speak(
                    locale = Locale.getDefault(),
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

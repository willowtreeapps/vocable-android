package com.willowtree.vocable.presets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.CategoriesUseCase
import com.willowtree.vocable.PhrasesUseCase
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PresetsViewModel(
    private val presetsRepository: IPresetsRepository,
    categoriesUseCase: CategoriesUseCase,
    private val phrasesUseCase: PhrasesUseCase
) : ViewModel() {

    val categoryList: LiveData<List<Category>> = categoriesUseCase.categories().asLiveData()

    // Will only ever be null immediately on init
    private val liveSelectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<Category?> = combine(
        categoriesUseCase.categories(),
        liveSelectedCategoryId
    ) { categories, selectedId ->
        categories.find { it.categoryId == selectedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)
    val selectedCategoryLiveData: LiveData<Category?> = selectedCategory.asLiveData()

    val currentPhrases: LiveData<List<Phrase?>> = liveSelectedCategoryId.map { categoryId ->
        if (categoryId == null) return@map emptyList<Phrase>()
        val phrases: MutableList<Phrase?> = phrasesUseCase.getPhrasesForCategory(categoryId)
            .run {
                if (categoryId != PresetCategories.RECENTS.id) {
                    sortedBy { it.sortOrder }
                } else {
                    this
                }
            }
            .toMutableList()
        //Add null to end of normal non empty category phrase list for the "+ Add Phrase" button
        if (categoryId != PresetCategories.RECENTS.id && categoryId != PresetCategories.USER_KEYPAD.id && phrases.isNotEmpty()) {
            phrases.add(null)
        }
        return@map phrases
    }.asLiveData()

    private val liveNavToAddPhrase = MutableLiveData<Boolean>()
    val navToAddPhrase: LiveData<Boolean> = liveNavToAddPhrase

    init {
        viewModelScope.launch {
            liveSelectedCategoryId.update { categoriesUseCase.categories().first().first().categoryId }
        }
    }

    fun onCategorySelected(categoryId: String) {
        liveSelectedCategoryId.update { categoryId }
    }

    fun addToRecents(phrase: Phrase) {
        viewModelScope.launch {
            phrasesUseCase.phraseSpoken(phrase.phraseId)
        }
    }

    fun navToAddPhrase() {
        liveNavToAddPhrase.value = true
        liveNavToAddPhrase.value = false
    }
}

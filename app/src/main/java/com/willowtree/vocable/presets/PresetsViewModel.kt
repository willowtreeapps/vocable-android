package com.willowtree.vocable.presets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.CategoriesUseCase
import com.willowtree.vocable.room.CategoryDto
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
import org.koin.core.component.inject

class PresetsViewModel : BaseViewModel() {

    private val presetsRepository: PresetsRepository by inject()
    private val categoriesUseCase: CategoriesUseCase by inject()

    val categoryList: LiveData<List<CategoryDto>> = categoriesUseCase.categories().asLiveData()

    // Will only ever be null immediately on init
    private val liveSelectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<CategoryDto?> = combine(
        categoriesUseCase.categories(),
        liveSelectedCategoryId
    ) { categories, selectedId ->
        categories.find { it.categoryId == selectedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)
    val selectedCategoryLiveData: LiveData<CategoryDto?> = selectedCategory.asLiveData()

    val currentPhrases: LiveData<List<Phrase?>> = liveSelectedCategoryId.map { categoryId ->
        if (categoryId == null) return@map emptyList<Phrase>()
        val phrases: MutableList<Phrase?> = if (categoryId == PresetCategories.RECENTS.id) {
            presetsRepository.getPhrasesForCategory(categoryId)
                .sortedBy { it.lastSpokenDate }.reversed().toMutableList()
        } else {
            presetsRepository.getPhrasesForCategory(categoryId)
                .sortedBy { it.sortOrder }.toMutableList()
        }
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
        backgroundScope.launch {
            presetsRepository.addPhraseToRecents(phrase)
        }
    }

    fun navToAddPhrase() {
        liveNavToAddPhrase.value = true
        liveNavToAddPhrase.value = false
    }
}

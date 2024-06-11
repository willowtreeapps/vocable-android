package com.willowtree.vocable.presets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.ICategoriesUseCase
import com.willowtree.vocable.IPhrasesUseCase
import com.willowtree.vocable.utils.IdlingResourceContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PresetsViewModel(
    private val categoriesUseCase: ICategoriesUseCase,
    private val phrasesUseCase: IPhrasesUseCase,
    private val idlingResourceContainer: IdlingResourceContainer
) : ViewModel() {

    val categoryList: LiveData<List<Category>> = categoriesUseCase.categories()
        .map { categories -> categories.filter { !it.hidden } }
        .asLiveData()

    // Will only ever be null immediately on init
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<Category?> = combine(
        categoriesUseCase.categories(),
        selectedCategoryId
    ) { categories, selectedId ->
        val currentCategory = categories.find { it.categoryId == selectedId }
        if (currentCategory?.hidden == true) {
            val newSortOrder = (currentCategory.sortOrder + 1)
            val newCategory = categories.find { it.sortOrder == newSortOrder }
            if (newCategory != null) {
                selectedCategoryId.update { newCategory.categoryId }
                categories.find { it.sortOrder == newSortOrder }
            } else {
                selectedCategoryId.update { categories.first().categoryId }
                categories.first()
            }
        } else {
            currentCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)
    val selectedCategoryLiveData: LiveData<Category?> = selectedCategory.asLiveData()

    val currentPhrases: LiveData<List<Phrase?>> = selectedCategoryId
        .filterNotNull()
        .flatMapLatest { categoryId ->
            phrasesUseCase.getPhrasesForCategoryFlow(categoryId).map { phrases ->
                val phrasesToReturn = phrases.run {
                    if (categoryId != PresetCategories.RECENTS.id) {
                        sortedBy { it.sortOrder }
                    } else {
                        this
                    }
                }.toMutableList<Phrase?>()
                if (categoryId != PresetCategories.RECENTS.id && categoryId != PresetCategories.USER_KEYPAD.id && phrases.isNotEmpty()) {
                    //Add null to end of normal non empty category phrase list for the "+ Add Phrase" button
                    phrasesToReturn.add(null)
                }
                phrasesToReturn
            }
        }
        .asLiveData()

    private val liveNavToAddPhrase = MutableLiveData<Boolean>()
    val navToAddPhrase: LiveData<Boolean> = liveNavToAddPhrase

    init {
        viewModelScope.launch {
            idlingResourceContainer.run {
                selectedCategoryId.update {
                    categoriesUseCase.categories().first().first().categoryId
                }
            }
        }
    }

    fun onCategorySelected(categoryId: String) {
        selectedCategoryId.update { categoryId }
    }

    fun addToRecents(phrase: Phrase) {
        viewModelScope.launch {
            idlingResourceContainer.run {
                phrasesUseCase.updatePhraseLastSpokenTime(phrase.phraseId)
            }
        }
    }

    fun navToAddPhrase() {
        liveNavToAddPhrase.value = true
        liveNavToAddPhrase.value = false
    }
}

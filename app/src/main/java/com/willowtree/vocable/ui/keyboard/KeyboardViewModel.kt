package com.willowtree.vocable.ui.keyboard

import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.domain.usecase.ICategoriesUseCase
import com.willowtree.vocable.domain.usecase.IPhrasesUseCase
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.ui.base.BaseViewModel
import com.willowtree.vocable.core.ILocalizedResourceUtility
import com.willowtree.vocable.core.VocableSharedPreferences
import com.willowtree.vocable.core.VocableTextToSpeech
import com.willowtree.vocable.core.locale.LocalesWithText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

/**
 * ViewModel for the [KeyboardScreen]. Handles text input, saving phrases, and responding to
 * user interactions. Also listens to shared preferences changes for head tracking toggle.
 */
class KeyboardViewModel : BaseViewModel<KeyboardState, KeyboardEvent>(KeyboardState()), KoinComponent {

    private val sharedPrefs: VocableSharedPreferences by inject()
    private val categoriesUseCase: ICategoriesUseCase by inject()
    private val phrasesUseCase: IPhrasesUseCase by inject()
    private val localizedResourceUtility: ILocalizedResourceUtility by inject()

    private val sharedPrefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == VocableSharedPreferences.KEY_HEAD_TRACKING_ENABLED) {
                updateHeadTrackingState()
            }
        }

    init {
        // Init state with shared prefs which acts as truth for the toggle
        updateHeadTrackingState()
        sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
    }
    
    fun setContext(
        categoryId: String? = null,
        isCategoryEdit: Boolean = false,
        phraseIdToEdit: String? = null,
        initialText: String? = null,
        saveCategoryId: String? = null
    ) {
        updateState { 
            copy(
                categoryIdToEdit = categoryId,
                isCategoryEdit = isCategoryEdit,
                phraseIdToEdit = phraseIdToEdit,
                inputText = initialText ?: "",
                saveCategoryId = saveCategoryId
            ) 
        }
    }

    fun updateHeadTrackingState() {
        val isEnabled = sharedPrefs.getHeadTrackingEnabled()
        updateState { copy(headTrackingEnabled = isEnabled) }
    }

    override fun onCleared() {
        super.onCleared()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)
    }

    fun onTextChange(newText: String) {
        updateState { copy(inputText = newText) }
    }

    fun onKey(key: String) {
        updateState {
            val current = inputText
            val newText = if (current.isEmpty()) {
                key
            } else {
                // If the string ends with a sentence terminator and a space, we capitalize the new key
                val isNewSentence = current.endsWith(". ") || 
                                    current.endsWith("? ") || 
                                    current.endsWith("! ")
                if (isNewSentence) {
                    current + key
                } else {
                    current + key.lowercase(Locale.getDefault())
                }
            }
            copy(inputText = newText)
        }
    }

    fun onSpace() {
        updateState { 
            // Avoid double spaces if already ends with space
            if (inputText.endsWith(" ")) {
                this
            } else {
                copy(inputText = "$inputText ")
            }
        }
    }

    fun onBackspace() {
        updateState {
            copy(inputText = if (inputText.isNotEmpty()) inputText.dropLast(1) else inputText)
        }
    }

    fun onClear() {
        updateState { copy(inputText = "") }
    }

    fun onSpeak() {
        if (uiState.value.isCategoryEdit || uiState.value.phraseIdToEdit != null) {
            // "Speak" action is mapped to "Save" in edit modes if we repurpose it or show a checkmark instead
            saveEdit()
            return
        }

        val text = uiState.value.inputText
        if (text.isNotBlank()) {
            VocableTextToSpeech.speak(Locale.getDefault(), text)
        }
    }
    
    fun saveEdit() {
        val state = uiState.value
        val text = state.inputText
        if (text.isBlank()) return
        
        viewModelScope.launch {
            if (state.isCategoryEdit) {
                 if (state.categoryIdToEdit != null) {
                    // rename
                    categoriesUseCase.updateCategoryName(
                        state.categoryIdToEdit,
                        LocalesWithText(mapOf(Locale.getDefault().toString() to text))
                    )
                 } else {
                    // add new
                    categoriesUseCase.addCategory(text)
                 }
                 sendEvent(KeyboardEvent.ShowToast("Category saved"))
                 sendEvent(KeyboardEvent.NavigateBack)
            } else if (state.phraseIdToEdit != null) {
                 phrasesUseCase.updatePhrase(
                    state.phraseIdToEdit,
                    text
                 )
                 sendEvent(KeyboardEvent.ShowToast("Phrase updated"))
                 sendEvent(KeyboardEvent.NavigateBack)
            }
        }
    }

    fun savePhrase() {
        val state = uiState.value
        if (state.isCategoryEdit || state.phraseIdToEdit != null) {
            saveEdit()
            return
        }

        val phraseText = state.inputText
        if (phraseText.isBlank()) return

        viewModelScope.launch {
            val categories = categoriesUseCase.categories().first()
            
            // Priority: Use provided category context, otherwise default to "My Sayings"
            val targetCategoryId = if (state.saveCategoryId != null) {
                state.saveCategoryId
            } else {
                var mySayingsCategory = categories.find { 
                    it is Category.StoredCategory && localizedResourceUtility.getTextFromCategory(it) == "My Sayings" 
                }
    
                if (mySayingsCategory == null) {
                    categoriesUseCase.addCategory("My Sayings")
                    val updatedCategories = categoriesUseCase.categories().first()
                    mySayingsCategory = updatedCategories.find { 
                        it is Category.StoredCategory && localizedResourceUtility.getTextFromCategory(it) == "My Sayings" 
                    }
                }
                mySayingsCategory?.categoryId
            }

            targetCategoryId?.let { categoryId ->
                val existing = phrasesUseCase.getPhrasesForCategory(categoryId)
                val alreadyExists = existing.any { phrase ->
                    localizedResourceUtility.getTextFromPhrase(phrase).equals(phraseText, ignoreCase = true)
                }

                if (alreadyExists) {
                    sendEvent(KeyboardEvent.ShowToast("Phrase already exists in this category"))
                } else {
                    phrasesUseCase.addPhrase(
                        LocalesWithText(mapOf(Locale.getDefault().toString() to phraseText)),
                        categoryId
                    )
                    sendEvent(KeyboardEvent.ShowToast("Phrase saved successfully"))
                    
                    // If we navigated here explicitly to save to a specific category, auto-close
                    if (state.saveCategoryId != null) {
                        sendEvent(KeyboardEvent.NavigateBack)
                    } else {
                        updateState { copy(inputText = "") }
                    }
                }
            }
        }
    }
}

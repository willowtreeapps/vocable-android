package com.willowtree.vocable.keyboard
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.PhrasesUseCase
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

class KeyboardFragmentViewModel : ViewModel(), KoinComponent {

    private val phrasesUseCase: PhrasesUseCase by inject()

    fun addNewPhrase(phraseStr: String, categoryId: String) {
        viewModelScope.launch {
            phrasesUseCase.addPhraseSpokenNow(
                LocalesWithText(mapOf(Pair(Locale.getDefault().toString(), phraseStr))),
                categoryId
            )
        }
    }
}



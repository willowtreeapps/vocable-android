package com.willowtree.vocable.keyboard

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.R
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.CategoryPhraseCrossRef
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.get
import org.koin.core.inject
import java.util.*

class KeyboardViewModel : BaseViewModel() {

    companion object {
        private const val PHRASE_ADDED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()
    private val mySayingsCategoryId: String =
        get<Context>().getString(R.string.category_my_sayings_id)

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    fun addNewPhrase(phraseStr: String) {
        backgroundScope.launch {
            val mySayingsCategory =
                presetsRepository.getCategoryById(mySayingsCategoryId)
            val phraseId = UUID.randomUUID().toString()
            val mySayingsPhrases =
                presetsRepository.getPhrasesForCategory(mySayingsCategory.categoryId)
            with(presetsRepository) {
                // TODO: Use currently set Locale
                addPhrase(
                    Phrase(
                        phraseId,
                        System.currentTimeMillis(),
                        true,
                        System.currentTimeMillis(),
                        mapOf(Pair(Locale.US.language, phraseStr)),
                        mySayingsPhrases.size
                    )
                )
                addCrossRef(
                    CategoryPhraseCrossRef(
                        mySayingsCategory.categoryId,
                        phraseId
                    )
                )
                if (mySayingsCategory.hidden) {
                    updateCategory(mySayingsCategory.apply {
                        hidden = false
                    })
                }
            }
            liveShowPhraseAdded.postValue(true)
            delay(PHRASE_ADDED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }
}
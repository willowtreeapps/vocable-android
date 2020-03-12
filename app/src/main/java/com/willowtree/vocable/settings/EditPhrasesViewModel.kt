package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.launch
import org.koin.core.inject

class EditPhrasesViewModel: BaseViewModel() {

    private val presetsRepository: PresetsRepository by inject()

    private val liveMySayingsList = MutableLiveData<List<Phrase>>()
    val mySayingsList: LiveData<List<Phrase>> = liveMySayingsList

    init {
        populateMySayings()
    }

    private fun populateMySayings() {
        backgroundScope.launch {

            val phrases = presetsRepository.getPhrasesForCategory(presetsRepository.getMySayingsId())

            liveMySayingsList.postValue(phrases)
        }
    }
}

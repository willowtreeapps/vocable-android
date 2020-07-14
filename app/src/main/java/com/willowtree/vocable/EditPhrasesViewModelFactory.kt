package com.willowtree.vocable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.settings.EditPhrasesViewModel

class EditPhrasesViewModelFactory(private var category: Category) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EditPhrasesViewModel(category) as T
    }


}
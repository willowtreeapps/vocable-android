package com.willowtree.vocable.utils

import com.willowtree.vocable.core.ILocalizedResourceUtility
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.domain.model.CustomPhrase
import com.willowtree.vocable.domain.model.Phrase
import com.willowtree.vocable.domain.model.PresetPhrase

class FakeLocalizedResourceUtility : ILocalizedResourceUtility {
    override fun getTextFromCategory(category: Category?): String {
        return when(category) {
            is Category.PresetCategory -> category.categoryId
            is Category.Recents -> "Recents"
            is Category.StoredCategory -> category.localizedName.localesTextMap.entries.first().value
            null -> ""
        }
    }

    override fun getTextFromPhrase(phrase: Phrase?): String {
        return when(phrase) {
            is CustomPhrase -> phrase.localizedUtterance?.localesTextMap?.entries?.first()?.value ?: ""
            is PresetPhrase -> phrase.phraseId
            null -> ""
        }
    }
}
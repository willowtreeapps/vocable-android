package com.willowtree.vocable.utils

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.CustomPhrase
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.presets.PresetPhrase

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
package com.willowtree.vocable.ui.languageselection

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.willowtree.vocable.core.IVocableSharedPreferences
import com.willowtree.vocable.ui.base.BaseViewModel
import java.util.Locale

class LanguageSelectionViewModel(
    private val sharedPreferences: IVocableSharedPreferences
) : BaseViewModel<LanguageSelectionState, LanguageSelectionEvent>(
    LanguageSelectionState(
        languages = buildLanguageList(),
        selectedLanguageTag = sharedPreferences.getSelectedLanguageTag()
    )
) {

    fun onLanguageSelected(tag: String?) {
        sharedPreferences.setSelectedLanguageTag(tag)
        updateState { copy(selectedLanguageTag = tag) }
        val localeList = if (tag.isNullOrEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
        sendEvent(LanguageSelectionEvent.NavigateBack)
    }
}

private fun buildLanguageList(): List<LanguageOption> {
    return SUPPORTED_LANGUAGE_TAGS.map { tag ->
        val locale = Locale.forLanguageTag(tag)
        LanguageOption(
            tag = tag,
            displayName = locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }
        )
    }
}

private val SUPPORTED_LANGUAGE_TAGS = listOf(
    "en",
    "ar-SA",
    "zh-Hans-CN",
    "zh-Hant-TW",
    "da-DK",
    "de",
    "es",
    "fr-FR",
    "hi",
    "it",
    "ja",
    "ko-KR",
    "pa",
    "ru-RU",
    "vi-VN"
)

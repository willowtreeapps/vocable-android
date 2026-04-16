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
        sharedPreferences.setSelectedVoiceName(null)
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
    // European
    "af-ZA", "sq-AL", "bg-BG", "ca-ES", "hr-HR", "cs-CZ", "da-DK",
    "nl-NL", "et-EE", "fi-FI", "fr", "fr-CA", "fr-BE", "fr-CH", "gl-ES", "de-DE",
    "de-AT", "de-CH", "el-GR", "hu-HU", "is-IS", "ga-IE", "it-IT", "lv-LV", "lt-LT",
    "mk-MK", "mt-MT", "nb-NO", "pl-PL", "pt", "pt-BR", "pt-PT",
    "ro-RO", "ru-RU", "sr-RS", "sk-SK", "sl-SI", "es", "es-AR", "es-CL",
    "es-CO", "es-MX", "es-PE", "es-US", "es-VE", "sv-SE", "tr-TR", "uk-UA", "cy-GB",
    // Middle East & Central Asia
    "ar", "ar-AE", "ar-EG", "ar-SA", "hy-AM", "az-AZ", "fa-IR", "ka-GE", "he-IL",
    "kk-KZ", "ur-PK", "uz-UZ",
    // South Asia
    "bn-BD", "bn-IN", "gu-IN", "hi-IN", "kn-IN", "ml-IN", "mr-IN", "ne-NP",
    "pa-IN", "si-LK", "ta-IN", "te-IN",
    // East & Southeast Asia
    "zh-CN", "zh-TW", "zh-HK", "ja-JP", "ko-KR", "id-ID", "ms-MY", "my-MM", "km-KH",
    "lo-LA", "mn-MN", "th-TH", "fil-PH", "vi-VN",
    // Africa
    "am-ET", "sw-KE", "zu-ZA",
)

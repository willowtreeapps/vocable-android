package com.willowtree.vocable.presets

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class PhraseGridItem : Parcelable {

    @Parcelize
    data class Phrase(
        val phraseId: String,
        val text: String
    ) : PhraseGridItem()

    @Parcelize
    object AddPhrase : PhraseGridItem()
}

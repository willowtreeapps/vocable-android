package com.willowtree.vocable.room

// TODO: MPV #467- this will take over the responsibilities for storing and fetching stored
//                 phrases from the [ILegacyCategoriesAndPhrasesRepository]
interface StoredPhrasesRepository {
    suspend fun addPhrase(phrase: PhraseDto)
    suspend fun updatePhraseLastSpokenTime(phraseId: String)
}
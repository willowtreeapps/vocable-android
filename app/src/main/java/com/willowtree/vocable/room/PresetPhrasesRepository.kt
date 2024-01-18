package com.willowtree.vocable.room

// TODO: MPV #467- this will take over the responsibilities for storing and fetching preset
//                 phrases from the [ILegacyCategoriesAndPhrasesRepository]
interface PresetPhrasesRepository {
    suspend fun addPhrase(phrase: PresetPhraseDto)
}
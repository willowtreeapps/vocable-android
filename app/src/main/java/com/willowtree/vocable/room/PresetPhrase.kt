package com.willowtree.vocable.room

data class PresetPhrase(
    val id: String,
    val categoryIds: List<String>,
    val localizedUtterance: Map<String, String>
)
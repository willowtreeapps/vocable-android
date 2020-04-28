package com.willowtree.vocable.room.models

data class PresetPhrase(
    val id: String,
    val categoryIds: List<String>,
    val localizedUtterance: Map<String, String>
)
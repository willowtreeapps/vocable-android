package com.willowtree.vocable.room.models

data class PresetsObject(
    val schemaVersion: Int,
    val phrases: List<PresetPhrase>,
    val categories: List<PresetCategory>
)
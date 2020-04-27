package com.willowtree.vocable.room.models

data class PresetCategory(
    val id: String,
    val localizedName: Map<String, String>,
    val hidden: Boolean
)
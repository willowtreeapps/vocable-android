package com.willowtree.vocable.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PresetPhrasesDao {

    @Query("SELECT * FROM PresetPhrase")
    suspend fun getAllPresetPhrases(): List<PresetPhraseDto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrases(phrases: List<PresetPhraseDto>)
}

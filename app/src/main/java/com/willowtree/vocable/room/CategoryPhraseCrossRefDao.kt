package com.willowtree.vocable.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface CategoryPhraseCrossRefDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryPhraseCrossRef(categoryPhraseCrossRef: CategoryPhraseCrossRef)

    @Delete
    suspend fun deleteCategoryPhraseCrossRefDao(categoryPhraseCrossRef: CategoryPhraseCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryPhraseCrossRefs(vararg crossRefs: CategoryPhraseCrossRef)
}
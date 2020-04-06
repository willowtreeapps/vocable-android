package com.willowtree.vocable.room

import androidx.room.*

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(vararg categories: Category)

    @Query("SELECT * FROM Category ORDER BY sort_order ASC")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM Category WHERE category_id = :categoryId")
    suspend fun getCategoryById(categoryId: String): Category

    @Update
    suspend fun updateCategory(category: Category)

    @Transaction
    @Query("SELECT * FROM Category WHERE category_id == :categoryId")
    suspend fun getCategoryWithPhrases(categoryId: String): List<CategoryWithPhrases>
}
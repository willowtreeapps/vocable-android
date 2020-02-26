package com.willowtree.vocable.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Category(
    @PrimaryKey val identifier: Long,
    @ColumnInfo(name = "creation_date") val creationDate: Long,
    @ColumnInfo(name = "is_user_generated") val isUserGenerated: Boolean,
    val name: String
)
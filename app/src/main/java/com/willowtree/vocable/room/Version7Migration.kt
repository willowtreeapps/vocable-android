package com.willowtree.vocable.room

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn(
    tableName = "Category",
    columnName = "resource_id"
)
class Version7Migration : AutoMigrationSpec
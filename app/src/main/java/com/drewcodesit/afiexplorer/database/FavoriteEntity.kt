package com.drewcodesit.afiexplorer.database

import androidx.room.*

//, indices = [Index(value = ["number", "title"], unique = true)]

@Entity(tableName = "favoritelist", indices = [Index(value = ["number", "title"], unique = true)])
data class FavoriteEntity (
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name ="id") var id: Int = 0,
    @ColumnInfo(name = "number") var Number: String? = null,
    @ColumnInfo(name = "title")  var Title: String? = null,
    @ColumnInfo(name = "url") var DocumentUrl: String? = null
)
package com.drewcodesit.afiexplorer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.drewcodesit.afiexplorer.utils.Config

@Entity(tableName = Config.TABLE_NAME, indices = [Index(value = ["number", "title"], unique = true)])
data class FavoriteEntity (
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name ="id") var id: Int = 0,
    @ColumnInfo(name = "number") var Number: String? = null,
    @ColumnInfo(name = "title")  var Title: String? = null,
    @ColumnInfo(name = "url") var DocumentUrl: String? = null,

)
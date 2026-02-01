package com.drewcodesit.afiexplorer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.drewcodesit.afiexplorer.utils.Config

@Entity(tableName = Config.TABLE_NAME, indices = [Index(value = ["number", "title"], unique = true)])
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name ="id") var id: Int = 0,
    @ColumnInfo(name = "number") var pubNumber: String = "",
    @ColumnInfo(name = "title")  var pubTitle: String = "",
    @ColumnInfo(name = "url") var pubDocumentUrl: String = ""
)

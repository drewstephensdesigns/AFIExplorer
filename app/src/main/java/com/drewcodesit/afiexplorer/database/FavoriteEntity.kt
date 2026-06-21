/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

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

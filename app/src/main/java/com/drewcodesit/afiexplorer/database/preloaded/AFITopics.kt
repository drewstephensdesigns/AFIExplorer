package com.drewcodesit.afiexplorer.database.preloaded

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.drewcodesit.afiexplorer.utils.Config

@Entity(tableName = Config.AFI_TOPICS_TABLE)
data class AFITopics(
    @PrimaryKey val pubId: Int,
    val title: String,
    val subtitle: String,
    val number: String,
    val url: String,
    val iconResId: Int
)

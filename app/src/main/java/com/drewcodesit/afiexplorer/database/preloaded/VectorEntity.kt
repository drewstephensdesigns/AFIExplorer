package com.drewcodesit.afiexplorer.database.preloaded

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.drewcodesit.afiexplorer.utils.Config
import com.drewcodesit.afiexplorer.utils.converters.FloatListConverter

@Entity(tableName = Config.VECTOR_TABLE)
@TypeConverters(FloatListConverter::class)
data class VectorEntity(
    @PrimaryKey val pubId: Int,
    val originalText: String,
    val embedding: List<Float>? = null  // <-- Add this
)

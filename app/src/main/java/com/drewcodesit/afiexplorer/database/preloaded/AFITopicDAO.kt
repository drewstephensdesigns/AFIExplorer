package com.drewcodesit.afiexplorer.database.preloaded

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.drewcodesit.afiexplorer.utils.Config
import kotlinx.coroutines.flow.Flow

@Dao
interface AFITopicDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topics: List<AFITopics>)

    @Query("SELECT * FROM ${Config.AFI_TOPICS_TABLE}")
    fun getAllTopics(): Flow<List<AFITopics>>
}
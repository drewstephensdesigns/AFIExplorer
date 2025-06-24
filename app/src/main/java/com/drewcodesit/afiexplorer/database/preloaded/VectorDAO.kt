package com.drewcodesit.afiexplorer.database.preloaded

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.drewcodesit.afiexplorer.utils.Config

@Dao
interface VectorDAO {

    @Insert (onConflict = OnConflictStrategy.REPLACE)
    suspend fun addData(vector: VectorEntity)

    @Query("SELECT * FROM ${Config.VECTOR_TABLE} WHERE pubId = :pubId")
    suspend fun getVectorsByPubId(pubId: Int): List<VectorEntity>

    @Query("SELECT DISTINCT pubId FROM ${Config.VECTOR_TABLE}")
    suspend fun getAllPubIds(): List<Int>

}
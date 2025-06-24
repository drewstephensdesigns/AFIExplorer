package com.drewcodesit.afiexplorer.database.favorites

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.drewcodesit.afiexplorer.utils.Config
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addData(favoriteEntity: FavoriteEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(favoriteEntity: FavoriteEntity)

    @Delete
    suspend fun delete(favoriteEntity: FavoriteEntity)

    @Query("SELECT * FROM ${Config.TABLE_NAME}")
    fun getFavoriteData(): Flow<List<FavoriteEntity>>

    @Query("DELETE FROM ${Config.TABLE_NAME}")
    suspend fun deleteAll()

    @Query("SELECT EXISTS (SELECT * FROM ${Config.TABLE_NAME} WHERE number=:number)")
    fun titleExists(number: String?): Int?
}

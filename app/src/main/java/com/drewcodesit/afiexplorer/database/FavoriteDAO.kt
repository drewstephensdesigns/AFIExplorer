package com.drewcodesit.afiexplorer.database

import androidx.room.*
import com.drewcodesit.afiexplorer.utils.Config

@Dao
interface FavoriteDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addData(favoriteEntity: FavoriteEntity?)

    @Query("SELECT * FROM ${Config.TABLE_NAME}")
    fun getFavoriteData(): MutableList<FavoriteEntity?>?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateFaves(favoriteEntity: FavoriteEntity?)

    @Delete
    fun delete(favoriteEntity: FavoriteEntity?)

    @Query("DELETE FROM ${Config.TABLE_NAME}")
    fun deleteAll()

    @Query("SELECT * FROM ${Config.TABLE_NAME} WHERE id LIKE :id")
    fun getPubs(id: Int): FavoriteEntity

    @Query("SELECT EXISTS (SELECT * FROM ${Config.TABLE_NAME} WHERE number=:number)")
    fun titleExists(number: String?): Int?

    @Query("SELECT * FROM ${Config.TABLE_NAME} ORDER BY number ASC")
    fun getAllSortedByName(): MutableList<FavoriteEntity?>?
}
/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package com.drewcodesit.afiexplorer.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameTable
import androidx.room.Room
import androidx.room.RoomDatabase
import com.drewcodesit.afiexplorer.utils.Config

@Database(
    entities = [FavoriteEntity::class, PubUpdateEntity::class],
    version = 10,
    autoMigrations = [AutoMigration (from = 9, to = 10)],
    exportSchema = true
)

abstract class FavoriteDatabase() : RoomDatabase(){
    @RenameTable(fromTableName = "favoritelist", toTableName = Config.TABLE_NAME)

    companion object {
        @Volatile
        var INSTANCE: FavoriteDatabase? = null

        fun getDatabase(context: Context): FavoriteDatabase {
            if (INSTANCE == null) synchronized(FavoriteDatabase::class.java){
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    FavoriteDatabase::class.java,
                    Config.DATABASE_NAME)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration(true)
                    .build()
            }
            return INSTANCE as FavoriteDatabase
        }
    }

    abstract fun favoriteDAO(): FavoriteDAO?
    abstract fun pubUpdateDAO(): PubUpdateDao?
}

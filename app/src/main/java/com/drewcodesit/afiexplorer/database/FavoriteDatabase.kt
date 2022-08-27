package com.drewcodesit.afiexplorer.database

import android.content.Context
import android.util.Log
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.drewcodesit.afiexplorer.utils.Config

@Database
    (entities = [FavoriteEntity::class],
    version = 2,
    autoMigrations = [
        AutoMigration (from = 1, to = 2)
    ],
    exportSchema = true)
abstract class FavoriteDatabase : RoomDatabase() {

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
                    .fallbackToDestructiveMigration()
                    .build()
                Log.i("Database", "Database created")
            }
            return INSTANCE as FavoriteDatabase
        }
    }

    abstract fun favoriteDAO(): FavoriteDAO?
}
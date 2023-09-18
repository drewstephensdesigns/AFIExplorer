package com.drewcodesit.afiexplorer.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameTable
import androidx.room.Room
import androidx.room.RoomDatabase
import com.drewcodesit.afiexplorer.utils.Config

@Database
    (entities = [FavoriteEntity::class],
    version = 3,
    autoMigrations = [
        AutoMigration (from = 2, to = 3)
    ],
    exportSchema = true)
abstract class FavoriteDatabase : RoomDatabase() {
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
                    .fallbackToDestructiveMigration()
                    .build()
                //Log.i("Database", "Database created")
            }
            return INSTANCE as FavoriteDatabase
        }
    }

    abstract fun favoriteDAO(): FavoriteDAO?
}
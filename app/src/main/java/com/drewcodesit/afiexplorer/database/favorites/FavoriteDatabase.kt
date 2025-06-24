package com.drewcodesit.afiexplorer.database.favorites

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.drewcodesit.afiexplorer.database.preloaded.AFITopicDAO
import com.drewcodesit.afiexplorer.database.preloaded.AFITopics
import com.drewcodesit.afiexplorer.database.preloaded.VectorDAO
import com.drewcodesit.afiexplorer.database.preloaded.VectorEntity
import com.drewcodesit.afiexplorer.utils.Config
import com.drewcodesit.afiexplorer.utils.converters.FloatListConverter

@Database(
    entities = [
        FavoriteEntity::class,
        AFITopics::class,
        VectorEntity::class
               ],
    version = 9,
    autoMigrations = [AutoMigration (from = 8, to = 9)],
    exportSchema = true
)

@TypeConverters(FloatListConverter::class)
abstract class FavoriteDatabase : RoomDatabase(){
    @RenameTable(fromTableName = "favoritelist", toTableName = Config.TABLE_NAME)

    companion object {
        @Volatile
        var INSTANCE: FavoriteDatabase? = null

        fun getDatabase(context: Context): FavoriteDatabase {
            return INSTANCE ?: synchronized(this){
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FavoriteDatabase::class.java,
                    Config.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    abstract fun favoriteDAO(): FavoriteDAO?
    abstract fun afiTopicDAO(): AFITopicDAO?
    abstract fun vectorDAO(): VectorDAO?
}

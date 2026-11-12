package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlayerProfileEntity::class,
        MatchHistoryEntity::class,
        AppSettingsEntity::class,
        SavedMatchEntity::class,
        DailyRewardEntity::class,
        TournamentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LudoDatabase : RoomDatabase() {
    abstract fun ludoDao(): LudoDao

    companion object {
        @Volatile
        private var INSTANCE: LudoDatabase? = null

        fun getInstance(context: Context): LudoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LudoDatabase::class.java,
                    "ludo_master_offline.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}

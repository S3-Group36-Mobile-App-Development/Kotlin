package com.zenmind.kotlin.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zenmind.kotlin.features.dailycheckin.data.db.CheckinDao
import com.zenmind.kotlin.features.dailycheckin.data.db.CheckinEntity
import com.zenmind.kotlin.features.dailycheckin.data.db.MoodDao
import com.zenmind.kotlin.features.dailycheckin.data.db.MoodEntity
import com.zenmind.kotlin.features.breathing.data.local.BreathingSessionEntity
import com.zenmind.kotlin.features.breathing.data.local.BreathingSessionDao

@Database(
    entities = [
        CheckinEntity::class, 
        MoodEntity::class,
        BreathingSessionEntity::class], 
    version = 4, 
    exportSchema = false
    )
abstract class AppDatabase : RoomDatabase() {

    abstract fun getCheckinDao(): CheckinDao
    abstract fun getMoodDao(): MoodDao
    abstract fun breathingSessionDao(): BreathingSessionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "zenmind_db")
                            .fallbackToDestructiveMigration(true)
                            .build()
                    }
                }
            }
            return instance!!
        }
    }
}

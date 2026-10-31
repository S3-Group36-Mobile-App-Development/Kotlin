package com.zenmind.kotlin.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zenmind.kotlin.features.dailycheckin.data.db.CheckinDao
import com.zenmind.kotlin.features.dailycheckin.data.db.CheckinEntity
import com.zenmind.kotlin.features.dailycheckin.data.db.MoodDao
import com.zenmind.kotlin.features.dailycheckin.data.db.MoodEntity
import com.zenmind.kotlin.features.breathing.data.local.BreathingSessionEntity
import com.zenmind.kotlin.features.breathing.data.local.BreathingSessionDao
import com.zenmind.kotlin.features.games.data.GameSessionDao
import com.zenmind.kotlin.features.games.data.GameSessionEntity

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS `game_sessions` (
            `id` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, `errors` INTEGER NOT NULL,
            `correctTaps` INTEGER NOT NULL, `completedAt` TEXT NOT NULL,
            `syncStatus` TEXT NOT NULL, `ownerId` TEXT NOT NULL,
            `isGuest` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
    }
}

@Database(
    entities = [
        CheckinEntity::class, 
        MoodEntity::class,
        BreathingSessionEntity::class,
        GameSessionEntity::class],
    version = 5,
    exportSchema = false
    )
abstract class AppDatabase : RoomDatabase() {

    abstract fun getCheckinDao(): CheckinDao
    abstract fun getMoodDao(): MoodDao
    abstract fun breathingSessionDao(): BreathingSessionDao
    abstract fun gameSessionDao(): GameSessionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "zenmind_db")
                            .addMigrations(MIGRATION_4_5)
                            .build()
                    }
                }
            }
            return instance!!
        }
    }
}

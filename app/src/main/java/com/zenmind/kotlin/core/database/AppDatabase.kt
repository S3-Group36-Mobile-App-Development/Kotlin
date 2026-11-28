package com.zenmind.kotlin.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import com.zenmind.kotlin.features.breathing.data.local.BreathingExerciseEntity
import com.zenmind.kotlin.features.breathing.data.local.BreathingExerciseDao
import com.zenmind.kotlin.features.panicbutton.data.ContactDao
import com.zenmind.kotlin.features.panicbutton.data.ContactEntity
import com.zenmind.kotlin.features.protocols.data.db.ProtocolDao
import com.zenmind.kotlin.features.protocols.data.db.ProtocolEntity
import com.zenmind.kotlin.features.protocols.data.db.StepsConverter
import com.zenmind.kotlin.features.flashcards.data.local.FlashcardDao
import com.zenmind.kotlin.features.flashcards.data.local.FlashcardReactionEntity
import com.zenmind.kotlin.features.flashcards.data.local.FlashcardTemaEntity
import com.zenmind.kotlin.features.flashcards.data.local.FlashcardCardEntity
import com.zenmind.kotlin.features.profile.data.local.ProfileDao
import com.zenmind.kotlin.features.profile.data.local.ProfileEntity
import com.zenmind.kotlin.features.profile.data.local.InstitutionEntity
import com.zenmind.kotlin.features.profile.data.local.DepartmentEntity
import com.zenmind.kotlin.features.profile.data.local.PendingProfileEntity

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS `game_sessions` (
            `id` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, `errors` INTEGER NOT NULL,
            `correctTaps` INTEGER NOT NULL, `completedAt` TEXT NOT NULL,
            `syncStatus` TEXT NOT NULL, `ownerId` TEXT NOT NULL,
            `isGuest` INTEGER NOT NULL, PRIMARY KEY(`id`))""")

        // Cache local del catalogo de ejercicios de respiracion.
        db.execSQL("""CREATE TABLE IF NOT EXISTS `breathing_exercises` (
            `id` INTEGER NOT NULL, `titulo` TEXT NOT NULL, `descripcion` TEXT,
            `duracion` INTEGER NOT NULL, `inhalar` INTEGER, `sostener` INTEGER,
            `exhalar` INTEGER, `sostener2` INTEGER, `ciclos` INTEGER NOT NULL,
            `vibra` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS `contacts` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `name` TEXT NOT NULL,
            `phone` TEXT NOT NULL)""")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `checkin_table` DROP COLUMN `isGuest`")
        db.execSQL("ALTER TABLE `mood_table` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS `pending_flashcard_reactions` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `flashcardId` TEXT NOT NULL,
            `tipo` TEXT NOT NULL,
            `synced` INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `flashcard_temas` (
            `id` TEXT NOT NULL PRIMARY KEY,
            `titulo` TEXT NOT NULL,
            `categoria` TEXT NOT NULL,
            `iconType` TEXT NOT NULL,
            `cardsCount` INTEGER NOT NULL,
            `totalLikes` INTEGER NOT NULL,
            `totalDislikes` INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `flashcard_cards` (
            `id` TEXT NOT NULL PRIMARY KEY,
            `temaId` TEXT NOT NULL,
            `titulo` TEXT NOT NULL,
            `contenido` TEXT NOT NULL,
            `fuente` TEXT NOT NULL,
            `likeCount` INTEGER NOT NULL,
            `dislikeCount` INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `protocol_table` (
            `id` INTEGER NOT NULL,
            `userId` TEXT NOT NULL,
            `titulo` TEXT NOT NULL,
            `esPredefinido` INTEGER NOT NULL,
            `pasos` TEXT NOT NULL,
            `synced` INTEGER NOT NULL,
            PRIMARY KEY(`id`, `userId`))""")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS `profiles` (
            `userId` TEXT NOT NULL, `email` TEXT NOT NULL, `name` TEXT NOT NULL,
            `createdAt` TEXT NOT NULL, `institutionId` TEXT, `departmentId` TEXT,
            `currentStreak` INTEGER, `bestStreak` INTEGER, PRIMARY KEY(`userId`))""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `institutions` (
            `id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, PRIMARY KEY(`id`))""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `departments` (
            `id` TEXT NOT NULL, `institutionId` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))""")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS `pending_profile_changes` (
            `userId` TEXT NOT NULL, `pendingName` TEXT, `pendingInstitutionId` TEXT,
            `pendingDepartmentId` TEXT, PRIMARY KEY(`userId`))""")
    }
}

@Database(
    entities = [
        CheckinEntity::class, 
        MoodEntity::class,
        BreathingSessionEntity::class,
        BreathingExerciseEntity::class,
        GameSessionEntity::class,
        ContactEntity::class,
        FlashcardReactionEntity::class,
        FlashcardTemaEntity::class,
        FlashcardCardEntity::class,
        ProtocolEntity::class,
        ProfileEntity::class,
        InstitutionEntity::class,
        DepartmentEntity::class,
        PendingProfileEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(StepsConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun getCheckinDao(): CheckinDao
    abstract fun getMoodDao(): MoodDao
    abstract fun breathingSessionDao(): BreathingSessionDao
    abstract fun breathingExerciseDao(): BreathingExerciseDao
    abstract fun gameSessionDao(): GameSessionDao
    abstract fun panicButtonDao(): ContactDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun protocolDao(): ProtocolDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "zenmind_db")
                            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                            .build()
                    }
                }
            }
            return instance!!
        }
    }
}

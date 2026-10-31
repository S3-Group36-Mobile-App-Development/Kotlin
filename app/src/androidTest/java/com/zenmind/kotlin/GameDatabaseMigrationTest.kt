package com.zenmind.kotlin

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zenmind.kotlin.core.database.AppDatabase
import com.zenmind.kotlin.core.database.MIGRATION_4_5
import com.zenmind.kotlin.features.games.data.GameSessionEntity
import com.zenmind.kotlin.features.games.data.GameOwner
import com.zenmind.kotlin.features.games.data.GameSyncStatus
import com.zenmind.kotlin.features.games.data.ownerFromToken
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameDatabaseMigrationTest {
    @Test fun upgradeFromFourPreservesCheckinsBreathingAndPersistsGamesOnReopen() {
        runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "game_migration_test"
        context.deleteDatabase(name)
        val old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        try {
            // Version 4 tables as declared by the existing Room entities.
            old.execSQL("""CREATE TABLE `checkin_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` TEXT NOT NULL, `moodId` INTEGER NOT NULL, `moodName` TEXT NOT NULL,
                `note` TEXT, `synced` INTEGER NOT NULL, `isGuest` INTEGER NOT NULL,
                `userId` TEXT NOT NULL)""")
            old.execSQL("CREATE TABLE `mood_table` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))")
            old.execSQL("""CREATE TABLE `breathing_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ejercicioId` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL, `synced` INTEGER NOT NULL)""")
            old.execSQL("INSERT INTO `mood_table` (`id`, `name`) VALUES (1, 'Calma')")
            old.execSQL("""INSERT INTO `checkin_table` (`date`,`moodId`,`moodName`,`note`,`synced`,`isGuest`,`userId`)
                VALUES ('2026-10-31',1,'Calma','conservar',0,1,'guest')""")
            old.execSQL("INSERT INTO `breathing_sessions` (`ejercicioId`,`completedAt`,`synced`) VALUES (2,123456,0)")
            old.version = 4
        } finally {
            old.close()
        }

        fun open(): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_4_5).build()

        try {
            val upgraded = open()
            try {
                assertEquals("conservar", upgraded.getCheckinDao().getByDate("2026-10-31", "guest")?.note)
                assertEquals("Calma", upgraded.getMoodDao().getAll().single().name)
                assertEquals(2, upgraded.breathingSessionDao().getPending().single().ejercicioId)
                upgraded.gameSessionDao().insert(GameSessionEntity(
                    id = "550e8400-e29b-41d4-a716-446655440000", durationMs = 5000,
                    errors = 1, correctTaps = 20, completedAt = "2026-10-31T12:00:00.000Z",
                    ownerId = "guest", isGuest = true
                ))
                upgraded.gameSessionDao().insert(GameSessionEntity(
                    id = "550e8400-e29b-41d4-a716-446655440000", durationMs = 5000,
                    errors = 1, correctTaps = 20, completedAt = "2026-10-31T12:00:00.000Z",
                    ownerId = "guest", isGuest = true
                ))
            } finally {
                upgraded.close()
            }
            val reopened = open()
            try {
                assertEquals(1, reopened.gameSessionDao().forOwner("guest", true).size)
                assertTrue(reopened.gameSessionDao().forOwner("user-1", false).isEmpty())
                assertTrue(reopened.gameSessionDao().forOwner("guest", false).isEmpty())
                assertEquals(0, reopened.gameSessionDao().markSynced("550e8400-e29b-41d4-a716-446655440000", "guest"))
                assertEquals(GameSyncStatus.PENDING, reopened.gameSessionDao().forOwner("guest", true).single().syncStatus)
                reopened.gameSessionDao().insert(GameSessionEntity(
                    id = "228f0a18-7810-4dad-acab-a48fbc03be29", durationMs = 6000,
                    errors = 0, correctTaps = 20, completedAt = "2026-10-31T12:01:00.000Z",
                    ownerId = "user-1", isGuest = false
                ))
                assertEquals(1, reopened.gameSessionDao().markSynced("228f0a18-7810-4dad-acab-a48fbc03be29", "user-1"))
                assertEquals(GameSyncStatus.SYNCED, reopened.gameSessionDao().forOwner("user-1", false).single().syncStatus)
                reopened.gameSessionDao().insert(GameSessionEntity(
                    id = "f22b622f-f9d0-4678-b6a6-485d448ee44d", durationMs = 7000,
                    errors = 1, correctTaps = 20, completedAt = "2026-10-31T12:02:00.000Z",
                    ownerId = "user-1", isGuest = false
                ))
                assertEquals(1, reopened.gameSessionDao().blockPendingForOwner("user-1", GameSyncStatus.AUTH_REQUIRED))
                assertTrue(reopened.gameSessionDao().pendingForOwner("user-1").isEmpty())
                assertEquals(1, reopened.gameSessionDao().restoreAfterLogin("user-1"))
                assertEquals(1, reopened.gameSessionDao().pendingForOwner("user-1").size)
                assertEquals(GameSyncStatus.PENDING, reopened.gameSessionDao().forOwner("guest", true).single().syncStatus)
                assertFalse(reopened.getCheckinDao().getByDate("2026-10-31", "guest")?.synced ?: true)
                assertEquals(1, reopened.breathingSessionDao().getPending().size)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
        }
    }

    @Test fun jwtSubSelectsOnlyTheAuthenticatedOwner() {
        val payload = Base64.encodeToString("{\"sub\":\"42\"}".toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        assertEquals(GameOwner("42", false), ownerFromToken("header.$payload.signature"))
        try {
            ownerFromToken("invalid")
            throw AssertionError("A malformed token cannot select an account")
        } catch (_: IllegalStateException) {
            // A broken token is not allowed to claim another account's games.
        }
    }
}

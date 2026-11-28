package com.zenmind.kotlin

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zenmind.kotlin.core.database.AppDatabase
import com.zenmind.kotlin.core.database.MIGRATION_8_9
import com.zenmind.kotlin.core.database.MIGRATION_9_10
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.authentication.data.models.UserDto
import com.zenmind.kotlin.features.profile.data.DepartmentDto
import com.zenmind.kotlin.features.profile.data.InstitutionDto
import com.zenmind.kotlin.features.profile.data.ProfileApiService
import com.zenmind.kotlin.features.profile.data.ProfileDto
import com.zenmind.kotlin.features.profile.data.ProfileRepository
import com.zenmind.kotlin.features.profile.data.ProfileStreakDto
import com.zenmind.kotlin.features.profile.data.local.PendingProfileEntity
import com.zenmind.kotlin.features.profile.data.UpdateInstitutionRequest
import com.zenmind.kotlin.features.profile.data.UpdateNameRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(AndroidJUnit4::class)
class ProfileDataTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
        TokenStorage(context).clearTokens()
    }

    @Test
    fun profileIsPersistentInRoomAndSeparatedByUser() = runBlocking {
        TokenStorage(context).saveTokens("token-1", "refresh")
        val api = FakeProfileApi()
        val repository = ProfileRepository(api, database.profileDao(), TokenStorage(context))

        repository.refreshProfile("1")
        assertEquals("Ana", repository.getLocalProfile("1")?.name)
        assertEquals(2, repository.getLocalProfile("1")?.currentStreak)
        assertNull(repository.getLocalProfile("2"))

        api.profile = api.profile.copy(id = "2", nombreVisible = "Beto")
        repository.refreshProfile("2")
        val secondRepository = ProfileRepository(api, database.profileDao(), TokenStorage(context))
        assertEquals("Ana", secondRepository.getLocalProfile("1")?.name)
        assertEquals("Beto", secondRepository.getLocalProfile("2")?.name)
        assertEquals(2, api.profileRequests)
    }

    @Test
    fun catalogDownloadsOnlyIfMissingAndDepartmentsUseArrayMap() = runBlocking {
        val api = FakeProfileApi()
        val repository = ProfileRepository(api, database.profileDao(), TokenStorage(context))

        assertEquals(1, repository.getInstitutions().size)
        assertEquals(1, repository.getInstitutions().size)
        assertEquals(1, api.catalogRequests)

        assertEquals("Ingeniería", repository.getDepartments("1").single().name)
        database.profileDao().clearDepartments()
        assertEquals("Ingeniería", repository.getDepartments("1").single().name)

        val newRepository = ProfileRepository(api, database.profileDao(), TokenStorage(context))
        assertEquals(1, newRepository.getInstitutions().size)
        assertEquals(1, api.catalogRequests)
        assertEquals(emptyList<Any>(), newRepository.getDepartments("1"))
    }

    @Test
    fun differentServerAccountCannotReplaceRequestedProfile() = runBlocking {
        TokenStorage(context).saveTokens("token-1", "refresh")
        val repository = ProfileRepository(FakeProfileApi(), database.profileDao(), TokenStorage(context))

        assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.refreshProfile("2") }
        }
        assertNull(repository.getLocalProfile("2"))
    }

    @Test
    fun confirmedNameSurvivesInstitutionFailureAndRetainsStreaks() = runBlocking {
        val payload = Base64.encodeToString("""{"sub":"1"}""".toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val token = "e30.$payload.sig"
        TokenStorage(context).saveTokens(token, "refresh")
        val api = FakeProfileApi().apply { expectedToken = token; institutionError = IOException("Sin red") }
        val repository = ProfileRepository(api, database.profileDao(), TokenStorage(context))
        repository.refreshProfile("1")

        repository.updateName("1", "Ana María")
        assertThrows(IOException::class.java) {
            runBlocking { repository.updateInstitution("1", "2", null) }
        }
        val saved = repository.getLocalProfile("1")!!
        assertEquals("Ana María", saved.name)
        assertEquals("1", saved.institutionId)
        assertEquals(2, saved.currentStreak)
        assertEquals(4, saved.bestStreak)

        api.institutionError = null
        repository.updateInstitution("1", "2", null)
        assertEquals("2", repository.getLocalProfile("1")?.institutionId)
        assertEquals(2, repository.getLocalProfile("1")?.currentStreak)
    }

    @Test
    fun lateProfileDownloadDoesNotOverwriteConfirmedLocalChanges() = runBlocking {
        val previous = database.profileDao().getProfile("1")
        val oldProfile = com.zenmind.kotlin.features.profile.data.local.ProfileEntity(
            "1", "ana@example.com", "Ana", "2026-01-01", null, null, 2, 4
        )
        database.profileDao().saveProfile(oldProfile.copy(name = "Ana María"))
        val downloaded = database.profileDao().saveDownloadedProfile(previous, oldProfile)
        assertEquals("Ana María", downloaded.name)
        assertEquals("Ana María", database.profileDao().getProfile("1")?.name)
    }

    @Test
    fun migrationAddsTablesWithoutDeletingExistingGameRecords() {
        val name = "profile-migration-test.db"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            old.execSQL("CREATE TABLE game_sessions (id TEXT NOT NULL PRIMARY KEY)")
            old.execSQL("INSERT INTO game_sessions (id) VALUES ('saved-game')")
            old.version = 8
        }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        MIGRATION_8_9.migrate(db)
                    }
                }).build()
        )
        try {
            val db = helper.writableDatabase
            db.query("SELECT id FROM game_sessions").use { cursor ->
                cursor.moveToFirst()
                assertEquals("saved-game", cursor.getString(0))
            }
            db.query("SELECT * FROM profiles").close()
            db.query("SELECT * FROM institutions").close()
            db.query("SELECT * FROM departments").close()
            assertEquals(9, db.version)
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migrationTo10PreservesProfileAndGameAndAddsPendingTable() {
        val name = "profile-pending-migration-test.db"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            old.execSQL("CREATE TABLE game_sessions (id TEXT NOT NULL PRIMARY KEY)")
            old.execSQL("INSERT INTO game_sessions (id) VALUES ('saved-game')")
            old.execSQL("""CREATE TABLE profiles (userId TEXT NOT NULL PRIMARY KEY, email TEXT NOT NULL,
                name TEXT NOT NULL, createdAt TEXT NOT NULL, institutionId TEXT, departmentId TEXT,
                currentStreak INTEGER, bestStreak INTEGER)""")
            old.execSQL("INSERT INTO profiles (userId, email, name, createdAt) VALUES ('1', 'ana@example.com', 'Ana', '2026-01-01')")
            old.version = 9
        }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        MIGRATION_9_10.migrate(db)
                    }
                }).build()
        )
        try {
            val db = helper.writableDatabase
            db.query("SELECT id FROM game_sessions").use { cursor ->
                cursor.moveToFirst()
                assertEquals("saved-game", cursor.getString(0))
            }
            db.query("SELECT name FROM profiles WHERE userId = '1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Ana", cursor.getString(0))
            }
            db.query("SELECT * FROM pending_profile_changes").close()
            assertEquals(10, db.version)
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun pendingChangesStaySeparateAndRetryOnlyFailedInstitution() = runBlocking {
        val payload = Base64.encodeToString("""{"sub":"1"}""".toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val token = "e30.$payload.sig"
        TokenStorage(context).saveTokens(token, "refresh", "1")
        val api = FakeProfileApi().apply { expectedToken = token; institutionError = IOException("Sin red") }
        val repository = ProfileRepository(api, database.profileDao(), TokenStorage(context))
        repository.refreshProfile("1")

        repository.markVerified("1")
        assertEquals(false, repository.syncPending("1"))
        assertEquals(0, api.nameRequests)
        assertEquals(0, api.institutionRequests)

        assertEquals(true, repository.savePendingChanges("1", "Ana María", "2", null))
        assertEquals(0, api.nameRequests)
        assertEquals("Ana María", repository.getPending("1")?.pendingName)
        assertEquals(false, repository.syncPending("1"))
        assertEquals("Ana María", repository.getLocalProfile("1")?.name)
        assertNull(repository.getPending("1")?.pendingName)
        assertEquals("2", repository.getPending("1")?.pendingInstitutionId)

        api.institutionError = null
        assertEquals(true, repository.syncPending("1"))
        assertEquals(1, api.nameRequests)
        assertEquals(2, api.institutionRequests)
        assertNull(repository.getPending("1"))
        assertEquals("2", repository.getLocalProfile("1")?.institutionId)
    }

    @Test
    fun confirmationOfOlderNameDoesNotEraseNewerPendingName() = runBlocking {
        val dao = database.profileDao()
        dao.savePending(PendingProfileEntity("1", "Ana María", null, null))
        dao.savePending(PendingProfileEntity("1", "Ana Luisa", null, null))

        dao.clearSentName("1", "Ana María")

        assertEquals("Ana Luisa", dao.getPending("1")?.pendingName)
        assertNull(dao.getPending("2"))
    }

    @Test
    fun pendingChangesSurviveClosingAndReopeningRoom() = runBlocking {
        val name = "pending-profile-persistence-test.db"
        context.deleteDatabase(name)
        val first = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            first.profileDao().savePending(PendingProfileEntity("1", "Ana María", "2", null))
        } finally {
            first.close()
        }
        val reopened = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            assertEquals("Ana María", reopened.profileDao().getPending("1")?.pendingName)
            assertEquals("2", reopened.profileDao().getPending("1")?.pendingInstitutionId)
            assertNull(reopened.profileDao().getPending("2"))
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun simultaneousSyncCallsSendOnlyOneRequest() = runBlocking {
        val payload = Base64.encodeToString("""{"sub":"1"}""".toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val token = "e30.$payload.sig"
        TokenStorage(context).saveTokens(token, "refresh", "1")
        val nameStarted = CompletableDeferred<Unit>()
        val releaseName = CompletableDeferred<Unit>()
        val api = FakeProfileApi().apply {
            expectedToken = token
            this.nameStarted = nameStarted
            waitOnName = releaseName
        }
        val repository = ProfileRepository(api, database.profileDao(), TokenStorage(context))
        repository.refreshProfile("1")
        repository.savePendingChanges("1", "Ana María", "1", "10")
        repository.markVerified("1")

        val first = async(Dispatchers.IO) { repository.syncPending("1") }
        nameStarted.await()
        assertEquals(false, repository.syncPending("1"))
        releaseName.complete(Unit)
        assertEquals(true, first.await())
        assertEquals(1, api.nameRequests)
        assertNull(repository.getPending("1"))
    }

    @Test
    fun offlineProfileRequiresMatchingStoredSessionAndCache() = runBlocking {
        val storage = TokenStorage(context)
        storage.saveTokens("token-1", "refresh", "1")
        val repository = ProfileRepository(FakeProfileApi(), database.profileDao(), storage)
        assertNull(repository.offlineSessionUserId())
        repository.refreshProfile("1")
        assertEquals("1", repository.offlineSessionUserId())
        storage.clearTokens()
        assertNull(repository.offlineSessionUserId())
    }

    @Test
    fun retrofitUsesExpectedPathsAuthorizationAndSeparateUpdateBodies() = runBlocking {
        val requests = mutableListOf<okhttp3.Request>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests.add(request)
            val json = when (request.url.encodedPath) {
                "/api/v1/instituciones" -> "[]"
                "/api/v1/usuarios/me" -> if (request.method == "GET") {
                    """{"id":"1","email":"ana@example.com","nombreVisible":"Ana","creadoEn":"2026-01-01","racha":{"rachaActualDias":2,"rachaMasLargaDias":4}}"""
                } else {
                    """{"id":"1","email":"ana@example.com","nombreVisible":"Ana","creadoEn":"2026-01-01"}"""
                }
                else -> """{"id":"1","email":"ana@example.com","nombreVisible":"Ana","creadoEn":"2026-01-01"}"""
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200)
                .message("OK").body(json.toResponseBody()).build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://example.test/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(ProfileApiService::class.java)

        assertEquals(2, api.getProfile("Bearer token-1").racha?.rachaActualDias)
        assertEquals(0, api.getInstitutions().size)
        api.updateName("Bearer token-1", UpdateNameRequest("Ana María"))
        api.updateInstitution("Bearer token-1", UpdateInstitutionRequest(1, 10))

        assertEquals(listOf("GET", "GET", "PATCH", "PUT"), requests.map { it.method })
        assertEquals(listOf("/api/v1/usuarios/me", "/api/v1/instituciones", "/api/v1/usuarios/me", "/api/v1/usuarios/me/institucion"),
            requests.map { it.url.encodedPath })
        assertEquals("Bearer token-1", requests[0].header("Authorization"))
        assertNull(requests[1].header("Authorization"))
        val nameBody = okio.Buffer().also { requests[2].body!!.writeTo(it) }.readUtf8()
        val institutionBody = okio.Buffer().also { requests[3].body!!.writeTo(it) }.readUtf8()
        assertEquals("Ana María", JSONObject(nameBody).getString("nombreVisible"))
        assertEquals(1, JSONObject(institutionBody).getInt("institucionId"))
        assertEquals(10, JSONObject(institutionBody).getInt("departamentoId"))
    }

    private class FakeProfileApi : ProfileApiService {
        var profileRequests = 0
        var catalogRequests = 0
        var expectedToken = "token-1"
        var institutionError: IOException? = null
        var nameRequests = 0
        var institutionRequests = 0
        var nameStarted: CompletableDeferred<Unit>? = null
        var waitOnName: CompletableDeferred<Unit>? = null
        var profile = ProfileDto("1", "ana@example.com", "Ana", "2026-01-01", "1", "10", ProfileStreakDto(2, 4))

        override suspend fun getProfile(authorization: String): ProfileDto {
            assertEquals("Bearer $expectedToken", authorization)
            profileRequests++
            return profile
        }

        override suspend fun getInstitutions(): List<InstitutionDto> {
            catalogRequests++
            return listOf(InstitutionDto("1", "Universidad", "universidad", listOf(DepartmentDto("10", "1", "Ingeniería"))))
        }

        override suspend fun updateName(authorization: String, request: UpdateNameRequest): UserDto {
            assertEquals("Bearer $expectedToken", authorization)
            nameRequests++
            nameStarted?.complete(Unit)
            waitOnName?.await()
            return userDto(request.nombreVisible, profile.institucionId, profile.departamentoId)
        }

        override suspend fun updateInstitution(authorization: String, request: UpdateInstitutionRequest): UserDto {
            assertEquals("Bearer $expectedToken", authorization)
            institutionRequests++
            institutionError?.let { throw it }
            return userDto(profile.nombreVisible, request.institucionId.toString(), request.departamentoId?.toString())
        }

        private fun userDto(name: String, institutionId: String?, departmentId: String?) =
            UserDto("1", profile.email, name, null, institutionId, departmentId,
                true, "es", false, profile.creadoEn)
    }
}

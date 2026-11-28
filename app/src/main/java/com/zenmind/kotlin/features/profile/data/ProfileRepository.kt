package com.zenmind.kotlin.features.profile.data

import android.util.ArrayMap
import android.util.Base64
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.profile.data.local.DepartmentEntity
import com.zenmind.kotlin.features.profile.data.local.InstitutionEntity
import com.zenmind.kotlin.features.profile.data.local.ProfileDao
import com.zenmind.kotlin.features.profile.data.local.ProfileEntity
import com.zenmind.kotlin.features.profile.data.local.PendingProfileEntity
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import retrofit2.HttpException

class ProfileRepository(
    private val api: ProfileApiService,
    private val dao: ProfileDao,
    private val tokenStorage: TokenStorage
) {
    private val departmentsByInstitution = ArrayMap<String, List<DepartmentEntity>>()
    private val cacheMutex = Mutex()
    private val syncMutex = Mutex()
    @Volatile private var verifiedUserId: String? = null

    fun markVerified(userId: String) {
        if (tokenStorage.getSessionUserId() == userId) verifiedUserId = userId
    }
    fun markUnverified() { verifiedUserId = null }

    fun isVerified(userId: String): Boolean = verifiedUserId == userId

    suspend fun offlineSessionUserId(): String? = withContext(Dispatchers.IO) {
        val userId = tokenStorage.getSessionUserId() ?: return@withContext null
        if (tokenStorage.hasSession() && dao.getProfile(userId) != null) userId else null
    }

    suspend fun savePendingChanges(userId: String, name: String, institutionId: String?, departmentId: String?): Boolean =
        withContext(Dispatchers.IO) {
            syncMutex.withLock {
                check(tokenStorage.getSessionUserId() == userId && tokenStorage.hasSession()) { "La cuenta activa cambió." }
                val confirmed = requireNotNull(dao.getProfile(userId)) { "No hay perfil local." }
                val pending = PendingProfileEntity(
                    userId, name.takeIf { it != confirmed.name },
                    institutionId.takeIf { it != confirmed.institutionId || departmentId != confirmed.departmentId },
                    departmentId
                )
                dao.replacePending(pending)
                pending.hasChanges
            }
        }

    suspend fun syncPending(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isVerified(userId) || tokenStorage.getSessionUserId() != userId || !syncMutex.tryLock()) {
            return@withContext false
        }
        try {
            val pending = dao.getPending(userId) ?: return@withContext false
            if (!pending.hasChanges) return@withContext false
            pending.pendingName?.let { name ->
                if (!isVerified(userId)) return@withContext false
                updateName(userId, name)
                dao.clearSentName(userId, name)
                dao.deleteIfEmpty(userId)
            }
            pending.pendingInstitutionId?.let { institutionId ->
                if (!isVerified(userId)) return@withContext false
                updateInstitution(userId, institutionId, pending.pendingDepartmentId)
                dao.clearSentInstitution(userId, institutionId, pending.pendingDepartmentId)
                dao.deleteIfEmpty(userId)
            }
            true
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            if (exception.code() == 401) markUnverified()
            false
        } catch (exception: IOException) {
            false
        } finally {
            syncMutex.unlock()
        }
    }

    suspend fun getPending(userId: String): PendingProfileEntity? = withContext(Dispatchers.IO) {
        dao.getPending(userId)
    }

    fun observePending(userId: String) = dao.observePending(userId).flowOn(Dispatchers.IO)

    suspend fun refreshProfile(userId: String): ProfileEntity = withContext(Dispatchers.IO) {
        val token = requireNotNull(tokenStorage.getAccessToken()) { "No hay sesión activa." }
        val previous = dao.getProfile(userId)
        val profile = api.getProfile("Bearer $token")
        check(profile.id == userId && tokenStorage.getAccessToken() == token) {
            "La sesión cambió mientras se consultaba el perfil."
        }
        dao.saveDownloadedProfile(previous, profile.toEntity())
    }

    suspend fun updateName(userId: String, name: String): ProfileEntity = withContext(Dispatchers.IO) {
        val token = requireNotNull(tokenStorage.getAccessToken()) { "No hay sesión activa." }
        check(tokenUserId(token) == userId) { "La cuenta activa cambió." }
        requireNotNull(dao.getProfile(userId)) { "No hay perfil local." }
        val confirmed = api.updateName("Bearer $token", UpdateNameRequest(name))
        check(confirmed.id == userId) { "La cuenta activa cambió." }
        dao.updateName(userId, confirmed.nombreVisible)
        requireNotNull(dao.getProfile(userId))
    }

    suspend fun updateInstitution(userId: String, institutionId: String, departmentId: String?): ProfileEntity =
        withContext(Dispatchers.IO) {
            val token = requireNotNull(tokenStorage.getAccessToken()) { "No hay sesión activa." }
            check(tokenUserId(token) == userId) { "La cuenta activa cambió." }
            requireNotNull(dao.getProfile(userId)) { "No hay perfil local." }
            val confirmed = api.updateInstitution(
                "Bearer $token", UpdateInstitutionRequest(institutionId.toLong(), departmentId?.toLong())
            )
            check(confirmed.id == userId) { "La cuenta activa cambió." }
            dao.updateInstitution(userId, confirmed.institucionId ?: institutionId, confirmed.departamentoId)
            requireNotNull(dao.getProfile(userId))
        }

    suspend fun getLocalProfile(userId: String): ProfileEntity? = withContext(Dispatchers.IO) {
        dao.getProfile(userId)
    }

    fun observeLocalProfile(userId: String) = dao.observeProfile(userId).flowOn(Dispatchers.IO)

    suspend fun getLocalInstitutions(): List<InstitutionEntity> = withContext(Dispatchers.IO) {
        dao.getInstitutions()
    }

    suspend fun getInstitutions(): List<InstitutionEntity> = withContext(Dispatchers.IO) {
        val local = dao.getInstitutions()
        if (local.isNotEmpty()) return@withContext local

        val remote = api.getInstitutions()
        val institutions = remote.map { InstitutionEntity(it.id, it.nombre, it.tipo) }
        val departments = remote.flatMap { institution ->
            institution.departamentos.map { DepartmentEntity(it.id, institution.id, it.nombre) }
        }
        dao.replaceCatalog(institutions, departments)
        cacheMutex.withLock { departmentsByInstitution.clear() }
        institutions
    }

    suspend fun getDepartments(institutionId: String): List<DepartmentEntity> = withContext(Dispatchers.IO) {
        cacheMutex.withLock { departmentsByInstitution[institutionId] }?.let { return@withContext it }
        val departments = dao.getDepartments(institutionId)
        cacheMutex.withLock {
            departmentsByInstitution[institutionId] ?: departments.also {
                departmentsByInstitution[institutionId] = it
            }
        }
    }

    private fun ProfileDto.toEntity() = ProfileEntity(
        userId = id,
        email = email,
        name = nombreVisible,
        createdAt = creadoEn,
        institutionId = institucionId,
        departmentId = departamentoId,
        currentStreak = racha?.rachaActualDias,
        bestStreak = racha?.rachaMasLargaDias
    )

    private fun tokenUserId(token: String): String? = try {
        val payload = token.split(".")[1]
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        JSONObject(String(decoded, Charsets.UTF_8)).getString("sub")
    } catch (exception: Exception) {
        null
    }
}

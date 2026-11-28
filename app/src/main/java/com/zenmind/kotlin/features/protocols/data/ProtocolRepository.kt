package com.zenmind.kotlin.features.protocols.data

import android.util.Base64
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.protocols.data.db.ProtocolDao
import com.zenmind.kotlin.features.protocols.data.db.toEntity
import com.zenmind.kotlin.features.protocols.data.db.toProtocol
import com.zenmind.kotlin.features.protocols.model.Protocol
import com.zenmind.kotlin.features.protocols.model.ProtocolStep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class ProtocolRepository(
    private val api: ProtocolApiService,
    private val dao: ProtocolDao,
    private val cache: ProtocolCache,
    private val tokenStorage: TokenStorage
) {
    private val syncMutex = Mutex()

    fun isGuest(): Boolean = tokenStorage.getAccessToken() == null

    suspend fun getLocalProtocols(): List<Protocol> =
        dao.getAll(userId()).map { it.toProtocol() }

    suspend fun refreshProtocols(): Boolean {
        if (isGuest()) return false
        return try {
            val userId = userId()
            val remote = api.getProtocols(auth()).map { it.toProtocol() }
            dao.replaceSynced(userId, remote.map { it.toEntity(userId) })
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getProtocol(id: Long): Protocol? {
        cache.get(id)?.let { return it }
        val protocol = dao.getById(id, userId())?.toProtocol() ?: return null
        cache.save(protocol)
        return protocol
    }

    suspend fun createProtocol(titulo: String, pasos: List<String>): Boolean {
        val userId = userId()
        val steps = pasos.mapIndexed { index, text -> ProtocolStep(index + 1, text) }
        return try {
            val created = api.createProtocol(auth(), requestOf(titulo, steps)).toProtocol()
            dao.insert(created.toEntity(userId))
            cache.save(created)
            true
        } catch (e: IOException) {
            val pending = Protocol(-System.currentTimeMillis(), titulo, false, steps, synced = false)
            dao.insert(pending.toEntity(userId))
            false
        }
    }

    suspend fun syncPending(): Int {
        if (isGuest()) return 0
        return syncMutex.withLock {
            val userId = userId()
            var sent = 0
            for (pending in dao.getPending(userId)) {
                try {
                    val created = api.createProtocol(auth(), requestOf(pending.titulo, pending.pasos)).toProtocol()
                    dao.delete(pending.id, userId)
                    dao.insert(created.toEntity(userId))
                    cache.remove(pending.id)
                    cache.save(created)
                    sent++
                } catch (e: IOException) {
                    break
                } catch (e: HttpException) {
                    break
                }
            }
            sent
        }
    }

    suspend fun registerView(id: Long) {
        if (isGuest() || id < 0) return
        try {
            api.registerView(auth(), id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
        }
    }

    private fun requestOf(titulo: String, steps: List<ProtocolStep>) =
        CreateProtocolRequest(titulo, steps.map { StepDto(it.ordenPaso, it.descripcion) })

    private fun auth(): String = "Bearer ${checkNotNull(tokenStorage.getAccessToken())}"

    private fun userId(): String {
        val token = tokenStorage.getAccessToken() ?: return GUEST_ID
        return try {
            val payload = token.split(".")[1]
            val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
            JSONObject(json).getString("sub")
        } catch (e: Exception) {
            GUEST_ID
        }
    }

    private companion object {
        const val GUEST_ID = "guest"
    }
}

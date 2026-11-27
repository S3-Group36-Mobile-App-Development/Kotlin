package com.zenmind.kotlin.features.protocols.data

import com.zenmind.kotlin.features.protocols.model.Protocol
import com.zenmind.kotlin.features.protocols.model.ProtocolStep
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ProtocolApiService {

    @GET("api/v1/protocolos")
    suspend fun getProtocols(
        @Header("Authorization") authorization: String
    ): List<ProtocolDto>

    @POST("api/v1/protocolos")
    suspend fun createProtocol(
        @Header("Authorization") authorization: String,
        @Body request: CreateProtocolRequest
    ): ProtocolDto
}

data class ProtocolDto(
    val id: String,
    val titulo: String,
    val esPredefinido: Boolean,
    val pasos: List<StepDto>
)

data class StepDto(
    val ordenPaso: Int,
    val descripcion: String
)

data class CreateProtocolRequest(
    val titulo: String,
    val pasos: List<StepDto>
)

fun ProtocolDto.toProtocol() = Protocol(
    id = id.toLong(),
    titulo = titulo,
    esPredefinido = esPredefinido,
    pasos = pasos.map { ProtocolStep(it.ordenPaso, it.descripcion) }
)

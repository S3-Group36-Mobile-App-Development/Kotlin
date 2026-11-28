package com.zenmind.kotlin.features.protocols.data

import com.zenmind.kotlin.features.protocols.model.Protocol
import com.zenmind.kotlin.features.protocols.model.ProtocolStep
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

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

    @POST("api/v1/protocolos/{id}/vistas")
    suspend fun registerView(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long
    ): Response<Unit>
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

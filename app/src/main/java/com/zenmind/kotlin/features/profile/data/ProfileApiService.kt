package com.zenmind.kotlin.features.profile.data

import com.zenmind.kotlin.features.authentication.data.models.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.PUT

interface ProfileApiService {
    @GET("api/v1/usuarios/me")
    suspend fun getProfile(@Header("Authorization") authorization: String): ProfileDto

    @PATCH("api/v1/usuarios/me")
    suspend fun updateName(
        @Header("Authorization") authorization: String,
        @Body request: UpdateNameRequest
    ): UserDto

    @PUT("api/v1/usuarios/me/institucion")
    suspend fun updateInstitution(
        @Header("Authorization") authorization: String,
        @Body request: UpdateInstitutionRequest
    ): UserDto

    @GET("api/v1/instituciones")
    suspend fun getInstitutions(): List<InstitutionDto>
}

data class ProfileDto(
    val id: String,
    val email: String,
    val nombreVisible: String,
    val creadoEn: String,
    val institucionId: String?,
    val departamentoId: String?,
    val racha: ProfileStreakDto?
)

data class ProfileStreakDto(val rachaActualDias: Int, val rachaMasLargaDias: Int)

data class InstitutionDto(
    val id: String,
    val nombre: String,
    val tipo: String,
    val departamentos: List<DepartmentDto>
)

data class DepartmentDto(val id: String, val institucionId: String, val nombre: String)

data class UpdateNameRequest(val nombreVisible: String)

data class UpdateInstitutionRequest(val institucionId: Long, val departamentoId: Long?)

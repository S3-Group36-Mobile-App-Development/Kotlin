package com.zenmind.kotlin.features.authentication.data.services

import com.zenmind.kotlin.features.authentication.data.models.AuthResponse
import com.zenmind.kotlin.features.authentication.data.models.LoginRequest
import com.zenmind.kotlin.features.authentication.data.models.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.zenmind.kotlin.features.authentication.data.models.RefreshRequest
import com.zenmind.kotlin.features.authentication.data.models.TokenResponse
import com.zenmind.kotlin.features.authentication.data.models.UserDto
import retrofit2.http.GET
import retrofit2.http.Header

interface AuthApiService {

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshRequest
    ): Response<TokenResponse>

    @GET("api/v1/users/me")
    suspend fun getCurrentUser(
        @Header("Authorization") authorization: String
    ): Response<UserDto>
}
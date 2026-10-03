package com.zenmind.kotlin.features.home.data

import retrofit2.http.GET
import retrofit2.http.Header

interface HomeApiService {

    @GET("api/v1/usuarios/me")
    suspend fun getProfile(@Header("Authorization") authorization: String): HomeProfileDto
}

data class HomeProfileDto(val nombreVisible: String?)

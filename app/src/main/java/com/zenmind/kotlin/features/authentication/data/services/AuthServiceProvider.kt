package com.zenmind.kotlin.features.authentication.data.services

import com.zenmind.kotlin.core.network.ApiClient

object AuthServiceProvider {

    val service: AuthApiService by lazy {
        ApiClient.retrofit.create(AuthApiService::class.java)
    }
}
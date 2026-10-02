package com.zenmind.kotlin.data

import com.zenmind.kotlin.features.home.data.HomeRepository
import com.zenmind.kotlin.features.home.data.NetworkHomeRepository

/** Contenedor de dependencias de la app (inyección manual). */
interface AppContainer {
    val homeRepository: HomeRepository
}

class DefaultAppContainer : AppContainer {
    override val homeRepository: HomeRepository by lazy {
        NetworkHomeRepository()
    }
}

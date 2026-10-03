package com.zenmind.kotlin

import android.app.Application
import com.zenmind.kotlin.data.AppContainer
import com.zenmind.kotlin.data.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ZenMindApplication : Application() {
    lateinit var container: AppContainer

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this, applicationScope)
    }
}

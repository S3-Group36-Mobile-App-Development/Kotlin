package com.zenmind.kotlin

import android.app.Application
import com.zenmind.kotlin.data.AppContainer
import com.zenmind.kotlin.data.DefaultAppContainer

class ZenMindApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}

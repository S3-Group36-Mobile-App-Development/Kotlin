package com.zenmind.kotlin.features.breathing.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.breathing.data.AccelerometerMotionSource
import com.zenmind.kotlin.features.breathing.data.BreathingRepository

/**
 * Crea el BreathingViewModel inyectandole el sensor del acelerometro.
 */
class BreathingViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val appContext = context.applicationContext

        val storage = TokenStorage(appContext)
        val repository = BreathingRepository(storage)
        val sensor = AccelerometerMotionSource(appContext)

        val myViewModel = BreathingViewModel(sensor, repository)

        return myViewModel as T
    }
}


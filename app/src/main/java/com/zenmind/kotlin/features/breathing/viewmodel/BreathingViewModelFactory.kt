package com.zenmind.kotlin.features.breathing.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenmind.kotlin.features.breathing.data.AccelerometerMotionSource

/**
 * Crea el BreathingViewModel inyectandole el sensor del acelerometro.
 * Compose necesita una Factory cuando el ViewModel tiene parametros
 * en el constructor.
 */
class BreathingViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val sensor = AccelerometerMotionSource(context.applicationContext)
        @Suppress("UNCHECKED_CAST")
        return BreathingViewModel(sensor) as T
    }
}

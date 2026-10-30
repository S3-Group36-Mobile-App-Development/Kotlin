package com.zenmind.kotlin.features.breathing.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenmind.kotlin.features.breathing.data.AccelerometerMotionSource
/**
 * Crea el BreathingViewModel inyectandole el sensor del acelerometro.
 */
class BreathingViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        
        val appContext = context.applicationContext

        val sensor = AccelerometerMotionSource(appContext)

        val myViewModel = BreathingViewModel(sensor)

        return myViewModel as T
    }
}


package com.zenmind.kotlin.features.breathing.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenmind.kotlin.core.database.AppDatabase
import com.zenmind.kotlin.features.breathing.data.AccelerometerMotionSource
import com.zenmind.kotlin.features.breathing.data.BreathingRepository

/**
 * Crea el BreathingViewModel inyectandole el sensor del acelerometro
 * y el repository de respiracion (Room + sincronizacion con el backend).
 */
class BreathingViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        val appContext = context.applicationContext

        val sensor = AccelerometerMotionSource(appContext)

        val dao = AppDatabase.getInstance(appContext).breathingSessionDao()
        val repository = BreathingRepository(dao, appContext)

        val myViewModel = BreathingViewModel(sensor, repository)

        @Suppress("UNCHECKED_CAST")
        return myViewModel as T
    }
}

package com.zenmind.kotlin.features.breathing.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenmind.kotlin.core.database.AppDatabase
import com.zenmind.kotlin.features.breathing.data.AccelerometerMotionSource
import com.zenmind.kotlin.features.breathing.data.BreathingRepository
import com.zenmind.kotlin.features.breathing.data.local.BreathingPreferences
import com.zenmind.kotlin.features.breathing.viewmodel.BreathingCatalogViewModel
import com.zenmind.kotlin.features.breathing.viewmodel.BreathingViewModel

/**
 * Crea el BreathingViewModel inyectandole el sensor del acelerometro
 * y el repository de respiracion (Room + sincronizacion con el backend).
 */
class BreathingViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        val appContext = context.applicationContext
        val db = AppDatabase.getInstance(appContext)

        val repository = BreathingRepository(
            dao = db.breathingSessionDao(),
            exerciseDao = db.breathingExerciseDao(),
            preferences = BreathingPreferences(appContext),
            context = appContext
        )

        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(BreathingCatalogViewModel::class.java) ->
                BreathingCatalogViewModel(repository) as T
            
            modelClass.isAssignableFrom(BreathingViewModel::class.java) ->
                BreathingViewModel(AccelerometerMotionSource(appContext), repository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}

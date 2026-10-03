package com.zenmind.kotlin.features.breathing.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.zenmind.kotlin.features.breathing.model.MotionState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.sqrt

/**
 * Lee el acelerometro del telefono y lo convierte a Still / Moving.
 * Envuelve el SensorManager de Android y expone el resultado como
 * un Flow, para que el ViewModel lo pueda escuchar con corutinas.
 */
class AccelerometerMotionSource(context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun motion(): Flow<MotionState> = callbackFlow {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val window = ArrayDeque<Float>()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)

                window.addLast(magnitude)
                if (window.size > 15) window.removeFirst()

                val moving = window.size >= 15 &&
                        (window.max() - window.min()) > 1.5f

                trySend(if (moving) MotionState.Moving else MotionState.Still)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(
                listener,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        } else {
            trySend(MotionState.Still)
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }.distinctUntilChanged()
}

package com.example.yetanothertimer.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

class MotionDetector(private val context: Context) : SensorEventListener {
    companion object {
        // Time threshold for "recent movement" in milliseconds
        const val MOTION_TIMEOUT_MS = 3000L
        // Acceleration threshold to detect movement (in m/s²)
        private const val MOTION_THRESHOLD = 1.5f
    }

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private val _isMoving = MutableStateFlow(false)
    val isMoving: StateFlow<Boolean> = _isMoving
    
    private var lastMotionTime = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var isInitialized = false

    fun startListening() {
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            if (isInitialized) {
                // Calculate the magnitude of acceleration change
                val deltaX = x - lastX
                val deltaY = y - lastY
                val deltaZ = z - lastZ
                val acceleration = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

                if (acceleration > MOTION_THRESHOLD) {
                    lastMotionTime = System.currentTimeMillis()
                }
            } else {
                // Initialize on first reading
                isInitialized = true
                lastMotionTime = System.currentTimeMillis()
            }

            lastX = x
            lastY = y
            lastZ = z

            // Update motion state based on time since last movement
            val currentTime = System.currentTimeMillis()
            val timeSinceMotion = currentTime - lastMotionTime
            _isMoving.value = timeSinceMotion < MOTION_TIMEOUT_MS
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
}
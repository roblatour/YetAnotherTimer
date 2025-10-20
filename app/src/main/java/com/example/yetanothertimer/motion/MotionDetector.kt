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
        // Allow brief gaps when accumulating sustained movement (sensor jitter/ticks)
        private const val GAP_TOLERANCE_MS = 400L
        // Asymmetric debounce durations
        private const val ENTER_MOVING_MS = 1500L  // ~1.5s of movement to consider moving
        // Exit uses MOTION_TIMEOUT_MS (3s of stillness) as requested
    }

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private val _isMoving = MutableStateFlow(false)
    val isMoving: StateFlow<Boolean> = _isMoving
    
    // Debounce timers to require sustained state before toggling
    private var movingCandidateStart = 0L
    private var stillCandidateStart = 0L
    private var lastMovingEventTime = 0L
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

            // Determine whether current reading indicates movement
            var movingNow = false
            if (isInitialized) {
                // Calculate the magnitude of acceleration change
                val deltaX = x - lastX
                val deltaY = y - lastY
                val deltaZ = z - lastZ
                val acceleration = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
                movingNow = acceleration > MOTION_THRESHOLD
            } else {
                // Initialize on first reading; don't treat first sample as movement
                isInitialized = true
            }

            lastX = x
            lastY = y
            lastZ = z

            // Apply symmetric debounce with small gap tolerance: require ~MOTION_TIMEOUT_MS
            // sustained movement (allowing short still gaps) to enter moving, and
            // ~MOTION_TIMEOUT_MS sustained stillness to exit moving.
            val currentTime = System.currentTimeMillis()
            if (movingNow) {
                lastMovingEventTime = currentTime
                // Clear stillness candidate while motion continues
                stillCandidateStart = 0L
                if (!_isMoving.value) {
                    if (movingCandidateStart == 0L) {
                        // Start accumulating movement window
                        movingCandidateStart = currentTime
                    }
                    // Use shorter window to enter moving state
                    if (currentTime - movingCandidateStart >= ENTER_MOVING_MS) {
                        _isMoving.value = true
                        movingCandidateStart = 0L
                    }
                } else {
                    // Already moving; nothing else to do
                }
            } else {
                if (!_isMoving.value) {
                    // When not yet moving, only reset the movement candidate if the gap
                    // since the last motion sample exceeds the gap tolerance
                    if (movingCandidateStart != 0L && lastMovingEventTime != 0L &&
                        currentTime - lastMovingEventTime > GAP_TOLERANCE_MS) {
                        movingCandidateStart = 0L
                    }
                } else {
                    // When currently moving, start/continue stillness window to exit
                    if (stillCandidateStart == 0L) stillCandidateStart = currentTime
                    if (currentTime - stillCandidateStart >= MOTION_TIMEOUT_MS) {
                        _isMoving.value = false
                        stillCandidateStart = 0L
                        movingCandidateStart = 0L
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
}
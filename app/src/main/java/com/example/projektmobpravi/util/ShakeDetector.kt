package com.example.projektmobpravi.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import kotlin.math.sqrt

class ShakeDetector(
    private val onSingleShake: () -> Unit,
    private val onDoubleShake: () -> Unit
) : SensorEventListener {

    companion object {
        private const val THRESHOLD_G       = 2.8f
        private const val EVENT_DEBOUNCE_MS = 400L  // ignorira sensor šum unutar jednog tresenja
        private const val DOUBLE_WINDOW_MS  = 650L  // dva tresenja unutar ovoga → dvostruko
        private const val POST_COOLDOWN_MS  = 1200L // pauza nakon što se akcija okine
    }

    private var lastEventTime   = 0L
    private var waitingForDouble = false
    private val handler = Handler(Looper.getMainLooper())

    private val fireSingle = Runnable {
        waitingForDouble = false
        onSingleShake()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val gX = event.values[0] / SensorManager.GRAVITY_EARTH
        val gY = event.values[1] / SensorManager.GRAVITY_EARTH
        val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        if (gForce < THRESHOLD_G) return

        val now = System.currentTimeMillis()
        if (now - lastEventTime < EVENT_DEBOUNCE_MS) return
        lastEventTime = now

        if (waitingForDouble) {
            handler.removeCallbacks(fireSingle)
            waitingForDouble = false
            lastEventTime = now + POST_COOLDOWN_MS
            onDoubleShake()
        } else {
            waitingForDouble = true
            handler.postDelayed(fireSingle, DOUBLE_WINDOW_MS)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun cancel() {
        handler.removeCallbacksAndMessages(null)
    }
}

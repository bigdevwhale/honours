package app.honours.ui.components

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import app.honours.BuildConfig

/** Where the light falls on a badge when the phone is held upright. */
val REST_LIGHT = Offset(-.5f, -.7f)

/**
 * Where the light comes from as the phone tilts, smoothed: tip it and the gloss slides across the
 * enamel like on a real pin. Screenshots and phones without the sensor keep the light still.
 */
@Composable
fun rememberLight(): State<Offset> {
    val ctx = LocalContext.current
    val light = remember { mutableStateOf(REST_LIGHT) }
    if (BuildConfig.SHOTS) return light
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var x = REST_LIGHT.x
        var y = REST_LIGHT.y
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                // Gravity along the screen's x and y; upright is (0, 9.8). Leaning right moves the light left.
                val tx = (-e.values[0] / 5f - .3f).coerceIn(-1f, 1f)
                val ty = ((e.values[1] - 8f) / 5f - .7f).coerceIn(-1f, 1f)
                x += (tx - x) * .15f
                y += (ty - y) * .15f
                light.value = Offset(x, y)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm?.unregisterListener(listener) }
    }
    return light
}

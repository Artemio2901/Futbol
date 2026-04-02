package dev.ricknout.composesensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Composable
@ReadOnlyComposable
fun getSensorManager(): SensorManager? {
    val context = LocalContext.current
    return ContextCompat.getSystemService(context, SensorManager::class.java)
}


@Composable
@ReadOnlyComposable
fun isSensorAvailable(type: Int): Boolean = getSensorInternal(type = type) != null

@Composable
@ReadOnlyComposable
fun getSensor(type: Int): Sensor = getSensorInternal(type = type)
    ?: throw RuntimeException("Sensor of type $type is not available, use one of the isSensorAvailable functions")

@Composable
@ReadOnlyComposable
internal fun getSensorInternal(type: Int): Sensor? {
    val sensorManager = getSensorManager() ?: throw RuntimeException("SensorManager is null")
    return sensorManager.getDefaultSensor(type)
}


@Composable
fun <T> rememberSensorValueAsState(
    type: Int,
    samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_NORMAL,
    transformSensorEvent: (event: SensorEvent?) -> T,
): State<T> {
    val context = LocalContext.current
    val sensorEventCallbackFlow = remember {
        sensorEventCallbackFlow(
            context = context,
            type = type,
            samplingPeriodUs = samplingPeriodUs,
        )
    }
    val sensorEvent by sensorEventCallbackFlow.collectAsStateWithLifecycle(
        initialValue = ComposableSensorEvent(),
        minActiveState = Lifecycle.State.RESUMED,
    )
    return remember { derivedStateOf { transformSensorEvent(sensorEvent.event) } }
}

class SensorValue<T>(
    val value: T,
    val timestamp: Long = SystemClock.elapsedRealtimeNanos(),
    val accuracy: Int = SensorManager.SENSOR_STATUS_NO_CONTACT,
) {
    companion object {
        val EMPTY_1D = SensorValue(value = 0f)
        val EMPTY_3D = SensorValue(value = Triple(0f, 0f, 0f))
    }
}

fun SensorEvent.to1DSensorValue(): SensorValue<Float> = SensorValue(
    value = values[0],
    timestamp = timestamp,
    accuracy = accuracy,
)


fun SensorEvent.to3DSensorValue(): SensorValue<Triple<Float, Float, Float>> = SensorValue(
    value = Triple(values[0], values[1], values[2]),
    timestamp = timestamp,
    accuracy = accuracy,
)

internal fun sensorEventCallbackFlow(
    context: Context,
    type: Int,
    samplingPeriodUs: Int,
): Flow<ComposableSensorEvent> = callbackFlow {
    val sensorManager = ContextCompat.getSystemService(context, SensorManager::class.java)
        ?: throw RuntimeException("SensorManager is null")
    val sensor = sensorManager.getDefaultSensor(type)
        ?: throw RuntimeException("Sensor of type $type is not available, use one of the isSensorAvailable functions")
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val composableEvent = ComposableSensorEvent(event = event)
            trySend(composableEvent)
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // TODO: Handle sensor accuracy changes?
        }
    }
    val successful = sensorManager.registerListener(listener, sensor, samplingPeriodUs)
    if (!successful) throw RuntimeException("Failed to register listener for sensor ${sensor.name}")
    awaitClose { sensorManager.unregisterListener(listener) }
}


internal data class ComposableSensorEvent(
    val event: SensorEvent? = null,
    val timestamp: Long = event?.timestamp ?: SystemClock.elapsedRealtimeNanos(),
)

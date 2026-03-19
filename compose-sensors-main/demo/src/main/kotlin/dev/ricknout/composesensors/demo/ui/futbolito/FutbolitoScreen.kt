package dev.ricknout.composesensors.demo.ui.futbolito

import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.ricknout.composesensors.accelerometer.isAccelerometerSensorAvailable
import dev.ricknout.composesensors.accelerometer.rememberAccelerometerSensorValueAsState

// ─── Colors ───────────────────────────────────────────────────────────────────
private val FieldGreen = Color(0xFF2E7D32)
private val BallWhite  = Color(0xFFFFFFFF)

// ─── Constants ────────────────────────────────────────────────────────────────
private const val BALL_RADIUS_DP  = 12f
/** How strongly the tilt moves the ball (px per (m/s²) per second). */
private const val ACCEL_FACTOR    = 120f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutbolitoScreen() {

    // ── Sensor ────────────────────────────────────────────────────────────────
    val sensorAvailable = isAccelerometerSensorAvailable()
    val sensorValue by rememberAccelerometerSensorValueAsState(
        samplingPeriodUs = SensorManager.SENSOR_DELAY_GAME
    )

    // ── Ball state ────────────────────────────────────────────────────────────
    // We cache these as Float refs to avoid recomposition on every frame.
    var ballX by remember { mutableStateOf(0f) }
    var ballY by remember { mutableStateOf(0f) }
    var initialised by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Futbolito ⚽") })
        }
    ) { paddingValues ->

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val ballRadius = BALL_RADIUS_DP.dp.toPx()
            val w = size.width
            val h = size.height

            // Centre ball the very first frame we know the canvas size
            if (!initialised && w > 0f && h > 0f) {
                ballX = w / 2f
                ballY = h / 2f
                initialised = true
            }

            // ── Field background ─────────────────────────────────────────────
            drawRect(color = FieldGreen)

            // ── Ball ─────────────────────────────────────────────────────────
            drawCircle(
                color = BallWhite,
                radius = ballRadius,
                center = Offset(ballX, ballY),
            )
        }

        // ── Game loop ─────────────────────────────────────────────────────────
        // Runs every frame (vsync) and updates ball position from sensor data.
        if (sensorAvailable) {
            LaunchedEffect(Unit) {
                var lastMs = 0L
                while (true) {
                    withFrameMillis { frameMs ->
                        if (lastMs == 0L) { lastMs = frameMs; return@withFrameMillis }
                        val dt = (frameMs - lastMs) / 1000f  // seconds
                        lastMs = frameMs

                        // Accelerometer axes (portrait):
                        //   x → positive = device tilted right  → ball moves right (+ x)
                        //   y → positive = device tilted back   → ball moves up (- y in canvas)
                        val (ax, ay, _) = sensorValue.value
                        ballX += ax * ACCEL_FACTOR * dt
                        ballY -= ay * ACCEL_FACTOR * dt
                    }
                }
            }
        }
    }
}

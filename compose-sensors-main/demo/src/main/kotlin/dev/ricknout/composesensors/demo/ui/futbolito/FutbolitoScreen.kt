package dev.ricknout.composesensors.demo.ui.futbolito

import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ricknout.composesensors.accelerometer.isAccelerometerSensorAvailable
import dev.ricknout.composesensors.accelerometer.rememberAccelerometerSensorValueAsState
import kotlinx.coroutines.delay

// ─── Colors ───────────────────────────────────────────────────────────────────
private val FieldGreen    = Color(0xFF2E7D32)
private val BallWhite     = Color(0xFFFFFFFF)
private val GoalYellow    = Color(0xFFFDD835)
private val ScoreBarColor = Color(0xCC1B5E20)

// ─── Constants ────────────────────────────────────────────────────────────────
private const val BALL_RADIUS_DP   = 12f
private const val GOAL_HEIGHT_DP   = 24f
private const val GOAL_WIDTH_RATIO = 0.35f   // 35% of field width
private const val ACCEL_FACTOR     = 120f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutbolitoScreen(vm: FutbolitoViewModel = viewModel()) {

    // ── Sensor ────────────────────────────────────────────────────────────────
    val sensorAvailable = isAccelerometerSensorAvailable()
    val sensorValue by rememberAccelerometerSensorValueAsState(
        samplingPeriodUs = SensorManager.SENSOR_DELAY_GAME
    )

    // ── Score ─────────────────────────────────────────────────────────────────
    val scoreTop    by vm.scoreTop.collectAsState()
    val scoreBottom by vm.scoreBottom.collectAsState()

    // ── Ball state ────────────────────────────────────────────────────────────
    var ballX       by remember { mutableStateOf(0f) }
    var ballY       by remember { mutableStateOf(0f) }
    var initialised by remember { mutableStateOf(false) }

    // ── Goal flash ────────────────────────────────────────────────────────────
    var showGol     by remember { mutableStateOf(false) }

    // ── Goal zones (computed when canvas size is known) ───────────────────────
    var topGoalRect    by remember { mutableStateOf(Rect.Zero) }
    var bottomGoalRect by remember { mutableStateOf(Rect.Zero) }
    // Guards against repeated goal detection on the same crossing
    var inTopGoal    by remember { mutableStateOf(false) }
    var inBottomGoal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Futbolito ⚽") })
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Field + Ball ───────────────────────────────────────────────
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ballRadius  = BALL_RADIUS_DP.dp.toPx()
                val goalHeight  = GOAL_HEIGHT_DP.dp.toPx()
                val goalWidth   = size.width * GOAL_WIDTH_RATIO
                val w = size.width
                val h = size.height

                // Initialise ball position and goal rects on first real frame
                if (!initialised && w > 0f && h > 0f) {
                    ballX = w / 2f
                    ballY = h / 2f
                    topGoalRect = Rect(
                        offset = Offset((w - goalWidth) / 2f, 0f),
                        size   = Size(goalWidth, goalHeight)
                    )
                    bottomGoalRect = Rect(
                        offset = Offset((w - goalWidth) / 2f, h - goalHeight),
                        size   = Size(goalWidth, goalHeight)
                    )
                    initialised = true
                }

                // Field background
                drawRect(color = FieldGreen)

                // Goal areas
                drawRect(
                    color   = GoalYellow,
                    topLeft = topGoalRect.topLeft,
                    size    = topGoalRect.size,
                )
                drawRect(
                    color   = GoalYellow,
                    topLeft = bottomGoalRect.topLeft,
                    size    = bottomGoalRect.size,
                )

                // Ball
                drawCircle(
                    color  = BallWhite,
                    radius = ballRadius,
                    center = Offset(ballX, ballY),
                )
            }

            // ── Scoreboard overlay ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(ScoreBarColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "🏠  $scoreBottom",
                    color      = Color.White,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text       = "VS",
                    color      = Color.White.copy(alpha = 0.6f),
                    fontSize   = 14.sp,
                )
                Text(
                    text       = "$scoreTop  🏚️",
                    color      = Color.White,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // ── ¡GOL! flash ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = showGol,
                enter   = fadeIn(),
                exit    = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text       = "⚽ ¡GOL!",
                    color      = Color.White,
                    fontSize   = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier   = Modifier
                        .background(Color(0x99000000), RoundedCornerShape(16.dp))
                        .padding(horizontal = 28.dp, vertical = 14.dp),
                )
            }
        }

        // ── Game loop ─────────────────────────────────────────────────────────
        if (sensorAvailable) {
            LaunchedEffect(Unit) {
                var lastMs = 0L
                while (true) {
                    withFrameMillis { frameMs ->
                        if (lastMs == 0L) { lastMs = frameMs; return@withFrameMillis }
                        val dt = (frameMs - lastMs) / 1000f
                        lastMs = frameMs

                        val (ax, ay, _) = sensorValue.value
                        ballX += ax * ACCEL_FACTOR * dt
                        ballY -= ay * ACCEL_FACTOR * dt

                        // ── Goal detection ────────────────────────────────
                        val ballRadius  = BALL_RADIUS_DP  // use raw dp for quick check
                        val centerInTop    = topGoalRect.contains(Offset(ballX, ballY))
                        val centerInBottom = bottomGoalRect.contains(Offset(ballX, ballY))

                        if (centerInTop && !inTopGoal) {
                            inTopGoal = true
                            vm.onGoalTop()
                            showGol = true
                        } else if (!centerInTop) {
                            inTopGoal = false
                        }

                        if (centerInBottom && !inBottomGoal) {
                            inBottomGoal = true
                            vm.onGoalBottom()
                            showGol = true
                        } else if (!centerInBottom) {
                            inBottomGoal = false
                        }
                    }
                }
            }

            // Hide ¡GOL! banner after 1.5 s
            LaunchedEffect(showGol) {
                if (showGol) {
                    delay(1500L)
                    showGol = false
                }
            }
        }
    }
}

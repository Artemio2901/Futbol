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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ricknout.composesensors.accelerometer.isAccelerometerSensorAvailable
import dev.ricknout.composesensors.accelerometer.rememberAccelerometerSensorValueAsState
import kotlinx.coroutines.delay

// ─── Colors ───────────────────────────────────────────────────────────────────
private val FieldGreenDark  = Color(0xFF1B5E20)
private val FieldGreenLight = Color(0xFF2E7D32)
private val LineWhite       = Color(0xCCFFFFFF)
private val GoalNetColor    = Color(0xFFFFFFFF)
private val GoalPostColor   = Color(0xFFFFEB3B)
private val BallWhite       = Color(0xFFFFFFFF)
private val BallShadow      = Color(0x44000000)
private val ScoreBarColor   = Color(0xDD0D3B1E)

// ─── Constants ────────────────────────────────────────────────────────────────
private const val BALL_RADIUS_DP    = 11f
private const val ACCEL_FACTOR      = 120f

/** Fraction of field width occupied by goal mouth */
private const val GOAL_W_RATIO      = 0.38f
/** Depth of the goal net drawn inside the field */
private const val GOAL_DEPTH_DP     = 28f

// ─── Field drawing helpers ────────────────────────────────────────────────────

private fun DrawScope.drawField() {
    val w = size.width
    val h = size.height

    // ── Striped background (alternating bands) ────────────────────────────────
    val stripes = 10
    val stripeH = h / stripes
    for (i in 0 until stripes) {
        drawRect(
            color   = if (i % 2 == 0) FieldGreenDark else FieldGreenLight,
            topLeft = Offset(0f, i * stripeH),
            size    = Size(w, stripeH),
        )
    }

    val lw = 3.dp.toPx()    // line width
    val stroke = Stroke(width = lw, cap = StrokeCap.Round)

    // ── Outer border ──────────────────────────────────────────────────────────
    val margin = 12.dp.toPx()
    drawRoundRect(
        color       = LineWhite,
        topLeft     = Offset(margin, margin),
        size        = Size(w - margin * 2, h - margin * 2),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style       = stroke,
    )

    // ── Center line ───────────────────────────────────────────────────────────
    drawLine(
        color       = LineWhite,
        start       = Offset(margin, h / 2f),
        end         = Offset(w - margin, h / 2f),
        strokeWidth = lw,
    )

    // ── Center circle ─────────────────────────────────────────────────────────
    val centerR = 52.dp.toPx()
    drawCircle(
        color  = LineWhite,
        radius = centerR,
        center = Offset(w / 2f, h / 2f),
        style  = stroke,
    )
    // Center dot
    drawCircle(
        color  = LineWhite,
        radius = 4.dp.toPx(),
        center = Offset(w / 2f, h / 2f),
    )

    // ── Penalty areas (top & bottom) ──────────────────────────────────────────
    val penW = w * 0.55f
    val penH = h * 0.15f
    val penX = (w - penW) / 2f

    // top penalty area
    drawRect(
        color   = LineWhite,
        topLeft = Offset(penX, margin),
        size    = Size(penW, penH),
        style   = stroke,
    )
    // bottom penalty area
    drawRect(
        color   = LineWhite,
        topLeft = Offset(penX, h - margin - penH),
        size    = Size(penW, penH),
        style   = stroke,
    )

    // ── Goal-area boxes (6-yard box, inside penalty area) ─────────────────────
    val gboxW = w * 0.32f
    val gboxH = h * 0.06f
    val gboxX = (w - gboxW) / 2f

    // top goal-area box
    drawRect(
        color   = LineWhite,
        topLeft = Offset(gboxX, margin),
        size    = Size(gboxW, gboxH),
        style   = stroke,
    )
    // bottom goal-area box
    drawRect(
        color   = LineWhite,
        topLeft = Offset(gboxX, h - margin - gboxH),
        size    = Size(gboxW, gboxH),
        style   = stroke,
    )

    // ── Penalty spots ─────────────────────────────────────────────────────────
    drawCircle(
        color  = LineWhite,
        radius = 4.dp.toPx(),
        center = Offset(w / 2f, margin + penH * 0.65f),
    )
    drawCircle(
        color  = LineWhite,
        radius = 4.dp.toPx(),
        center = Offset(w / 2f, h - margin - penH * 0.65f),
    )

    // ── Corner arcs ───────────────────────────────────────────────────────────
    val cornerR = 14.dp.toPx()
    // Top-left
    drawArc(color = LineWhite, startAngle = 0f,   sweepAngle = 90f,  useCenter = false,
        topLeft = Offset(margin - cornerR, margin - cornerR),
        size    = Size(cornerR * 2, cornerR * 2), style = stroke)
    // Top-right
    drawArc(color = LineWhite, startAngle = 90f,  sweepAngle = 90f,  useCenter = false,
        topLeft = Offset(w - margin - cornerR, margin - cornerR),
        size    = Size(cornerR * 2, cornerR * 2), style = stroke)
    // Bottom-left
    drawArc(color = LineWhite, startAngle = 270f, sweepAngle = 90f,  useCenter = false,
        topLeft = Offset(margin - cornerR, h - margin - cornerR),
        size    = Size(cornerR * 2, cornerR * 2), style = stroke)
    // Bottom-right
    drawArc(color = LineWhite, startAngle = 180f, sweepAngle = 90f,  useCenter = false,
        topLeft = Offset(w - margin - cornerR, h - margin - cornerR),
        size    = Size(cornerR * 2, cornerR * 2), style = stroke)
}

private fun DrawScope.drawGoals() {
    val w = size.width
    val h = size.height
    val goalW      = w * GOAL_W_RATIO
    val goalDepth  = GOAL_DEPTH_DP.dp.toPx()
    val goalX      = (w - goalW) / 2f
    val postWidth  = 5.dp.toPx()
    val netStroke  = Stroke(
        width       = 1.2f.dp.toPx(),
        pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
    )

    // ── TOP GOAL ──────────────────────────────────────────────────────────────
    // Net (dashed fill effect - horizontal lines)
    val netLinesTop = 4
    for (i in 1..netLinesTop) {
        val y = i * goalDepth / (netLinesTop + 1)
        drawLine(
            color       = GoalNetColor.copy(alpha = 0.35f),
            start       = Offset(goalX, y),
            end         = Offset(goalX + goalW, y),
            strokeWidth = 1.2f.dp.toPx(),
            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)),
        )
    }
    // Vertical net lines
    val netColsTop = 5
    for (i in 1..netColsTop) {
        val x = goalX + i * goalW / (netColsTop + 1)
        drawLine(
            color       = GoalNetColor.copy(alpha = 0.35f),
            start       = Offset(x, 0f),
            end         = Offset(x, goalDepth),
            strokeWidth = 1.2f.dp.toPx(),
        )
    }
    // Goal posts (yellow)
    drawLine(
        color       = GoalPostColor,
        start       = Offset(goalX, 0f),
        end         = Offset(goalX, goalDepth),
        strokeWidth = postWidth,
        cap         = StrokeCap.Round,
    )
    drawLine(
        color       = GoalPostColor,
        start       = Offset(goalX + goalW, 0f),
        end         = Offset(goalX + goalW, goalDepth),
        strokeWidth = postWidth,
        cap         = StrokeCap.Round,
    )
    // Crossbar
    drawLine(
        color       = GoalPostColor,
        start       = Offset(goalX, goalDepth),
        end         = Offset(goalX + goalW, goalDepth),
        strokeWidth = postWidth,
        cap         = StrokeCap.Round,
    )

    // ── BOTTOM GOAL ───────────────────────────────────────────────────────────
    val netLinesBot = 4
    for (i in 1..netLinesBot) {
        val y = h - i * goalDepth / (netLinesBot + 1)
        drawLine(
            color       = GoalNetColor.copy(alpha = 0.35f),
            start       = Offset(goalX, y),
            end         = Offset(goalX + goalW, y),
            strokeWidth = 1.2f.dp.toPx(),
            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)),
        )
    }
    val netColsBot = 5
    for (i in 1..netColsBot) {
        val x = goalX + i * goalW / (netColsBot + 1)
        drawLine(
            color       = GoalNetColor.copy(alpha = 0.35f),
            start       = Offset(x, h - goalDepth),
            end         = Offset(x, h),
            strokeWidth = 1.2f.dp.toPx(),
        )
    }
    drawLine(
        color       = GoalPostColor,
        start       = Offset(goalX, h),
        end         = Offset(goalX, h - goalDepth),
        strokeWidth = postWidth,
        cap         = StrokeCap.Round,
    )
    drawLine(
        color       = GoalPostColor,
        start       = Offset(goalX + goalW, h),
        end         = Offset(goalX + goalW, h - goalDepth),
        strokeWidth = postWidth,
        cap         = StrokeCap.Round,
    )
    drawLine(
        color       = GoalPostColor,
        start       = Offset(goalX, h - goalDepth),
        end         = Offset(goalX + goalW, h - goalDepth),
        strokeWidth = postWidth,
        cap         = StrokeCap.Round,
    )
}

// ─── Main composable ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutbolitoScreen(vm: FutbolitoViewModel = viewModel()) {

    val sensorAvailable = isAccelerometerSensorAvailable()
    val sensorValue by rememberAccelerometerSensorValueAsState(
        samplingPeriodUs = SensorManager.SENSOR_DELAY_GAME
    )

    val scoreTop    by vm.scoreTop.collectAsState()
    val scoreBottom by vm.scoreBottom.collectAsState()

    var ballX       by remember { mutableStateOf(0f) }
    var ballY       by remember { mutableStateOf(0f) }
    var initialised by remember { mutableStateOf(false) }
    var showGol     by remember { mutableStateOf(false) }

    // Goal zones — updated once canvas size is known
    var topGoalRect    by remember { mutableStateOf(Rect.Zero) }
    var bottomGoalRect by remember { mutableStateOf(Rect.Zero) }
    var inTopGoal      by remember { mutableStateOf(false) }
    var inBottomGoal   by remember { mutableStateOf(false) }

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

            // ── Field canvas ──────────────────────────────────────────────
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ballRadius = BALL_RADIUS_DP.dp.toPx()
                val goalDepth  = GOAL_DEPTH_DP.dp.toPx()
                val goalW      = size.width * GOAL_W_RATIO
                val goalX      = (size.width - goalW) / 2f
                val w = size.width
                val h = size.height

                if (!initialised && w > 0f && h > 0f) {
                    ballX = w / 2f
                    ballY = h / 2f
                    topGoalRect    = Rect(Offset(goalX, 0f),        Size(goalW, goalDepth))
                    bottomGoalRect = Rect(Offset(goalX, h - goalDepth), Size(goalW, goalDepth))
                    initialised = true
                }

                // Draw field, goals, then ball
                drawField()
                drawGoals()

                // Ball shadow
                drawCircle(
                    color  = BallShadow,
                    radius = ballRadius + 3.dp.toPx(),
                    center = Offset(ballX + 3.dp.toPx(), ballY + 3.dp.toPx()),
                )
                // Ball
                drawCircle(
                    color  = BallWhite,
                    radius = ballRadius,
                    center = Offset(ballX, ballY),
                )
                // Ball shine
                drawCircle(
                    color  = Color.White.copy(alpha = 0.6f),
                    radius = ballRadius * 0.35f,
                    center = Offset(ballX - ballRadius * 0.28f, ballY - ballRadius * 0.28f),
                )
            }

            // ── Scoreboard ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(ScoreBarColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 28.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "🏠  $scoreBottom",
                    color      = Color.White,
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text     = "—",
                    color    = Color.White.copy(alpha = 0.5f),
                    fontSize = 18.sp,
                )
                Text(
                    text       = "$scoreTop  🏚️",
                    color      = Color.White,
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // ── ¡GOL! banner ──────────────────────────────────────────────
            AnimatedVisibility(
                visible  = showGol,
                enter    = fadeIn(),
                exit     = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text       = "⚽  ¡GOL!",
                    color      = Color.White,
                    fontSize   = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier   = Modifier
                        .background(Color(0xAA000000), RoundedCornerShape(18.dp))
                        .padding(horizontal = 32.dp, vertical = 16.dp),
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

                        // Goal detection
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

            LaunchedEffect(showGol) {
                if (showGol) {
                    delay(1500L)
                    showGol = false
                }
            }
        }
    }
}

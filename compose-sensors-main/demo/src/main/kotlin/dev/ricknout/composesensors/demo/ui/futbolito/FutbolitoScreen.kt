package dev.ricknout.composesensors.demo.ui.futbolito

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ─── Colors ──────────────────────────────────────────────────────────────────
private val FieldGreen  = Color(0xFF2E7D32)
private val BallWhite   = Color(0xFFFFFFFF)

// ─── Dimensions ──────────────────────────────────────────────────────────────
private const val BALL_RADIUS_DP = 12f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutbolitoScreen() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Futbolito") })
        }
    ) { paddingValues ->
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Field background ──────────────────────────────────────────
            drawRect(color = FieldGreen)

            // ── Ball (static, centered) ───────────────────────────────────
            val ballRadius = BALL_RADIUS_DP.dp.toPx()
            drawCircle(
                color = BallWhite,
                radius = ballRadius,
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }
    }
}

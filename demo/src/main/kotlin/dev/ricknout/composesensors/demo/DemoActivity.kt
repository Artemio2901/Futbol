package dev.ricknout.composesensors.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dev.ricknout.composesensors.demo.ui.DemoTheme
import dev.ricknout.composesensors.demo.ui.futbolito.FutbolitoScreen

class DemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            DemoTheme {
                FutbolitoScreen()
            }
        }
    }
}

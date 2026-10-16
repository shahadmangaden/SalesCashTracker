package com.jash.salescashtracker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jash.salescashtracker.ui.AppRoot
import com.jash.salescashtracker.ui.theme.SalesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app always uses its light design, so keep dark status/navigation bar icons
        // even when the phone is in dark mode (otherwise the clock and battery turn white and disappear).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        com.jash.salescashtracker.ui.ThemeState.load(this)
        setContent {
            SalesTheme {
                AppRoot()
            }
        }
    }
}

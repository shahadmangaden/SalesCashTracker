package com.jash.salescashtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jash.salescashtracker.ui.AppRoot
import com.jash.salescashtracker.ui.theme.SalesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SalesTheme {
                AppRoot()
            }
        }
    }
}

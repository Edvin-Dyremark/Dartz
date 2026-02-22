package com.dyre.dartz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dyre.dartz.navigation.DartsNavGraph
import com.dyre.dartz.ui.theme.DartzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DartzTheme {
                DartsNavGraph()
            }
        }
    }
}

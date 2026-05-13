package com.sanidad.movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sanidad.movil.data.presentation.navigation.NavGraph
import com.sanidad.movil.presentation.theme.SanidadMovilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SanidadMovilTheme {
                NavGraph()
            }
        }
    }
}
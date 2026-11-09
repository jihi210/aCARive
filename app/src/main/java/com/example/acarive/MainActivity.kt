package com.example.acarive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.acarive.ui.ACARiveApp
import com.example.acarive.ui.theme.ACARiveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ACARiveTheme {
                ACARiveApp()
            }
        }
    }
}

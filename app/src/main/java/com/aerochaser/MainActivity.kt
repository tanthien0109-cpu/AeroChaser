package com.aerochaser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aerochaser.ui.theme.AeroChaserTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AeroChaserTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                        // We would inject the viewmodel via Koin here, using stub for scaffolding
                        // val viewModel: TimelineViewModel by koinViewModel()
                        Text(text = "AeroChaser Timeline Screen Wrapper", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}

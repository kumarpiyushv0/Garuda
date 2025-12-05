package com.example.garuda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.garuda.ui.navigation.GarudaNavGraph
import com.example.garuda.ui.theme.GarudaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GarudaTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Passing innerPadding to NavGraph might be needed if screens need to respect it, 
                    // or we handle Scaffold inside screens.
                    // For now, let's wrap NavGraph in a Box or similar to apply padding, 
                    // or pass it down. 
                    // Simplest for now: apply padding to the graph container.
                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
                         GarudaNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
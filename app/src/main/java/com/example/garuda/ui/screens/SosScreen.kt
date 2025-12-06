package com.example.garuda.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.garuda.service.PowerButtonService
import com.example.garuda.service.SosService
import com.example.garuda.ui.components.EmergencyHelplineBar
import com.example.garuda.ui.navigation.Screen
import com.example.garuda.ui.theme.SafetyRed

@Composable
fun SosScreen(navController: NavController) {
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            EmergencyHelplineBar()
        },
        containerColor = SafetyRed
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SOS TRIGGERED!",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Sending Alerts...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = {
                    // Stop SosService
                    val sosIntent = Intent(context, SosService::class.java).apply {
                        action = SosService.ACTION_STOP
                    }
                    context.stopService(sosIntent)

                    // Stop PowerButtonService
                    val powerIntent = Intent(context, PowerButtonService::class.java).apply {
                        action = PowerButtonService.ACTION_STOP
                    }
                    context.stopService(powerIntent)

                    // Stop VoiceActivationService
                    val voiceIntent = Intent(context, com.example.garuda.service.VoiceActivationService::class.java).apply {
                        action = com.example.garuda.service.VoiceActivationService.ACTION_STOP
                    }
                    context.stopService(voiceIntent)

                    // Navigate to Home
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Sos.route) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("I AM SAFE", color = SafetyRed)
            }
        }
    }
}



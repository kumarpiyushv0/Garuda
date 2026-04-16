package com.example.garuda.ui.screens

import android.R.attr.top
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.garuda.service.PowerButtonService
import com.example.garuda.service.VoiceActivationService
import com.example.garuda.ui.components.EmergencyHelplineBar
import com.example.garuda.ui.components.SosButton
import com.example.garuda.ui.navigation.Screen
import com.example.garuda.ui.viewmodel.SosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: SosViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var isPowerButtonListenerEnabled by remember { mutableStateOf(false) }
    var isVoiceActivationEnabled by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            EmergencyHelplineBar()
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "GARUD",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 32.dp)
                    .align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Tap for Emergency",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            SosButton(
                onClick = {
                    viewModel.onSosClicked()
                    navController.navigate(Screen.Sos.route)
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Power Button Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Power Button SOS",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Press power 4x quickly",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = isPowerButtonListenerEnabled,
                        onCheckedChange = { enabled ->
                            isPowerButtonListenerEnabled = enabled
                            val intent = Intent(context, PowerButtonService::class.java).apply {
                                action = if (enabled) {
                                    PowerButtonService.ACTION_START
                                } else {
                                    PowerButtonService.ACTION_STOP
                                }
                            }
                            if (enabled) {
                                context.startForegroundService(intent)
                            } else {
                                context.stopService(intent)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Voice Activation Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Voice SOS",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Say 'Hey Garuda Help!'",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = isVoiceActivationEnabled,
                        onCheckedChange = { enabled ->
                            isVoiceActivationEnabled = enabled
                            val intent = Intent(context, VoiceActivationService::class.java).apply {
                                action = if (enabled) {
                                    VoiceActivationService.ACTION_START
                                } else {
                                    VoiceActivationService.ACTION_STOP
                                }
                            }
                            if (enabled) {
                                context.startForegroundService(intent)
                            } else {
                                context.stopService(intent)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Navigation Buttons for MVP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { navController.navigate(Screen.Contacts.route) }) {
                    Text("Contacts")
                }
                Button(onClick = { navController.navigate(Screen.Recordings.route) }) {
                    Text("Recordings")
                }
                Button(onClick = { navController.navigate(Screen.Map.route) }) {
                    Text("Map")
                }
            }
        }
    }
}



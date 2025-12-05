package com.example.garuda.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.garuda.ui.components.SosButton
import com.example.garuda.ui.navigation.Screen
import com.example.garuda.ui.viewmodel.SosViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: SosViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
        
        // Navigation Buttons for MVP
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { navController.navigate(Screen.Contacts.route) }) {
                Text("Contacts")
            }
            Button(onClick = { navController.navigate(Screen.Map.route) }) {
                Text("Map")
            }
        }
    }
}

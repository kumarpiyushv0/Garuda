@file:Suppress("UNUSED_VALUE", "UNUSED_VARIABLE")

package com.example.garuda.ui.screens

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class AudioRecording(
    val file: File,
    val name: String,
    val duration: String,
    val date: String,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(navController: NavController) {
    val context = LocalContext.current
    var recordings by remember { mutableStateOf<List<AudioRecording>>(emptyList()) }
    var currentlyPlaying by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    // Load recordings from cache directory
    LaunchedEffect(Unit) {
        recordings = loadRecordings(context.cacheDir)
    }
    
    // Cleanup media player on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Recordings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No recordings yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Emergency audio recordings will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recordings, key = { it.file.absolutePath }) { recording ->
                    RecordingItem(
                        recording = recording,
                        isPlaying = currentlyPlaying == recording.file.absolutePath,
                        onPlayPause = {
                            if (currentlyPlaying == recording.file.absolutePath) {
                                // Stop playing
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                currentlyPlaying = null
                            } else {
                                // Start playing
                                mediaPlayer?.release()
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(recording.file.absolutePath)
                                    prepare()
                                    start()
                                    setOnCompletionListener {
                                        currentlyPlaying = null
                                    }
                                }
                                currentlyPlaying = recording.file.absolutePath
                            }
                        },
                        onDelete = {
                            // Stop if currently playing
                            if (currentlyPlaying == recording.file.absolutePath) {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                currentlyPlaying = null
                            }
                            // Delete the file
                            recording.file.delete()
                            recordings = recordings.filter { it.file.absolutePath != recording.file.absolutePath }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingItem(
    recording: AudioRecording,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause Button
            FilledIconButton(
                onClick = onPlayPause,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow, // use PlayArrow for both states to avoid missing Pause icon
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Recording Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = recording.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = recording.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Delete Button
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Recording?") },
            text = { Text("This action cannot be undone. The recording will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun loadRecordings(cacheDir: File): List<AudioRecording> {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    return cacheDir.listFiles { file ->
        file.name.startsWith("emergency_audio_") && file.name.endsWith(".3gp")
    }?.map { file ->
        // Extract timestamp from filename: emergency_audio_1234567890.3gp
        val timestamp = file.name
            .removePrefix("emergency_audio_")
            .removeSuffix(".3gp")
            .toLongOrNull() ?: file.lastModified()
        
        // Get duration using MediaPlayer
        val duration = try {
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            val durationMs = mp.duration
            mp.release()
            formatDuration(durationMs)
        } catch (e: Exception) {
            Log.e("RecordingsScreen", "Error reading audio duration", e)
            "Unknown"
        }

        AudioRecording(
            file = file,
            name = "Emergency Recording",
            duration = duration,
            date = dateFormat.format(Date(timestamp)),
            timestamp = timestamp
        )
    }?.sortedByDescending { it.timestamp } ?: emptyList()
}

private fun formatDuration(durationMs: Int): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / 1000) / 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

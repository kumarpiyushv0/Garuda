package com.example.garuda.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import org.json.JSONObject
import java.net.URL

// Data classes for places
data class NearbyPlace(
    val name: String,
    val address: String,
    val location: LatLng,
    val type: PlaceType,
    val rating: Double?,
    val isOpen: Boolean?,
    val placeId: String
)

enum class PlaceType(
    val displayName: String,
    val searchQuery: String,
    val markerColor: Float
) {
    POLICE("Police Station", "police", BitmapDescriptorFactory.HUE_BLUE),
    HOSPITAL("Hospital", "hospital", BitmapDescriptorFactory.HUE_RED),
    PHARMACY("Pharmacy", "pharmacy", BitmapDescriptorFactory.HUE_GREEN),
    FIRE_STATION("Fire Station", "fire_station", BitmapDescriptorFactory.HUE_ORANGE),
    WOMEN_HELPLINE("Women Helpline", "women+helpline+center", BitmapDescriptorFactory.HUE_VIOLET)
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController) {
    val context = LocalContext.current

    // Permission state
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    
    // Map state
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var nearbyPlaces by remember { mutableStateOf<List<NearbyPlace>>(emptyList()) }
    var selectedType by remember { mutableStateOf(PlaceType.POLICE) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedPlace by remember { mutableStateOf<NearbyPlace?>(null) }
    
    // Camera position state
    val cameraPositionState = rememberCameraPositionState()
    
    // Get user location on start
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            userLocation = getCurrentLocation(context)
            userLocation?.let { location ->
                cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 14f)
                // Load initial places
                isLoading = true
                nearbyPlaces = searchNearbyPlaces(context, location, selectedType)
                isLoading = false
            }
        }
    }
    
    // Load places when type changes
    LaunchedEffect(selectedType) {
        userLocation?.let { location ->
            isLoading = true
            nearbyPlaces = searchNearbyPlaces(context, location, selectedType)
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby Help") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        if (!locationPermissionState.status.isGranted) {
            // Permission not granted UI
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Replaced large location icon with text
                    Text(
                        "Location Off",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Location permission is required",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Place type filter chips
                PlaceTypeChips(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )
                
                // Map
                Box(modifier = Modifier.weight(1f)) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = true
                        ),
                        uiSettings = MapUiSettings(
                            myLocationButtonEnabled = true,
                            zoomControlsEnabled = true
                        )
                    ) {
                        // Place markers
                        nearbyPlaces.forEach { place ->
                            Marker(
                                state = MarkerState(position = place.location),
                                title = place.name,
                                snippet = place.address,
                                icon = BitmapDescriptorFactory.defaultMarker(place.type.markerColor),
                                onClick = {
                                    selectedPlace = place
                                    true
                                }
                            )
                        }
                    }
                    
                    // Selected place card at bottom
                    selectedPlace?.let { place ->
                        PlaceDetailCard(
                            place = place,
                            onClose = { selectedPlace = null },
                            onNavigate = {
                                openGoogleMapsNavigation(context, place.location)
                            },
                            onCall = {
                                // Open dialer with emergency number based on type
                                val number = when (place.type) {
                                    PlaceType.POLICE -> "100"
                                    PlaceType.HOSPITAL -> "108"
                                    PlaceType.FIRE_STATION -> "101"
                                    PlaceType.WOMEN_HELPLINE -> "181"
                                    else -> "112"
                                }
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        )
                    }
                }
                
                // Quick emergency numbers bar
                EmergencyNumbersBar()
            }
        }
    }
}

@Composable
private fun PlaceTypeChips(
    selectedType: PlaceType,
    onTypeSelected: (PlaceType) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(PlaceType.entries) { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.displayName) }
            )
        }
    }
}

@Composable
private fun PlaceDetailCard(
    place: NearbyPlace,
    onClose: () -> Unit,
    onNavigate: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Replaced type icon with a small text label showing the type
                        Text(
                            text = place.type.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = place.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Rating and Open status
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        place.rating?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Replaced star icon with a star character
                                Text(
                                    "★",
                                    color = Color(0xFFFFB300),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f", it),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        place.isOpen?.let {
                            Text(
                                text = if (it) "Open" else "Closed",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (it) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                    }
                }
                
                IconButton(onClick = onClose) {
                    // Close replaced with text
                    Text("Close")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Navigate")
                }
                OutlinedButton(
                    onClick = onCall,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Call")
                }
            }
        }
    }
}

@Composable
private fun EmergencyNumbersBar() {
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EmergencyNumber(
                label = "Police",
                number = "100",
                onClick = { dialNumber(context, "100") }
            )
            EmergencyNumber(
                label = "Ambulance",
                number = "108",
                onClick = { dialNumber(context, "108") }
            )
            EmergencyNumber(
                label = "Women",
                number = "181",
                onClick = { dialNumber(context, "181") }
            )
            EmergencyNumber(
                label = "Fire",
                number = "101",
                onClick = { dialNumber(context, "101") }
            )
        }
    }
}

@Composable
private fun EmergencyNumber(
    label: String,
    number: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Removed icon - replaced by label text above number
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Text(
            text = number,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
    }
}

private fun dialNumber(context: Context, number: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
    context.startActivity(intent)
}

private fun openGoogleMapsNavigation(context: Context, destination: LatLng) {
    val uri = Uri.parse("google.navigation:q=${destination.latitude},${destination.longitude}&mode=d")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // Fallback to browser
        val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${destination.latitude},${destination.longitude}")
        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
    }
}

@SuppressLint("MissingPermission")
private suspend fun getCurrentLocation(context: Context): LatLng? {
    return try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val location = fusedLocationClient.lastLocation.await()
        location?.let { LatLng(it.latitude, it.longitude) }
    } catch (e: Exception) {
        null
    }
}

private suspend fun searchNearbyPlaces(
    context: Context,
    location: LatLng,
    type: PlaceType
): List<NearbyPlace> = withContext(Dispatchers.IO) {
    try {
        val apiKey = context.getString(com.example.garuda.R.string.api_key)
        val radius = 5000 // 5km radius
        
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${location.latitude},${location.longitude}" +
                "&radius=$radius" +
                "&type=${type.searchQuery}" +
                "&key=$apiKey"
        
        val response = URL(url).readText()
        val json = JSONObject(response)
        val results = json.optJSONArray("results") ?: return@withContext emptyList()
        
        val places = mutableListOf<NearbyPlace>()
        for (i in 0 until minOf(results.length(), 15)) {
            val place = results.getJSONObject(i)
            val geometry = place.getJSONObject("geometry")
            val placeLocation = geometry.getJSONObject("location")
            
            places.add(
                NearbyPlace(
                    name = place.getString("name"),
                    address = place.optString("vicinity", "Address not available"),
                    location = LatLng(
                        placeLocation.getDouble("lat"),
                        placeLocation.getDouble("lng")
                    ),
                    type = type,
                    rating = if (place.has("rating")) place.getDouble("rating") else null,
                    isOpen = place.optJSONObject("opening_hours")?.optBoolean("open_now"),
                    placeId = place.getString("place_id")
                )
            )
        }
        places
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

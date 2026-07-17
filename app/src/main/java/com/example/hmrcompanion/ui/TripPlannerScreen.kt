package com.example.hmrcompanion.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPlannerScreen(
    viewModel: TripPlannerViewModel,
    onTripStarted: (lineKey: String, fromStation: String, toStation: String) -> Unit,
    onTripStopped: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }
    var showSettingsButton by remember { mutableStateOf(false) }

    val permissionsToRequest = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.POST_NOTIFICATIONS] == true
            } else {
                true
            }

            when {
                locationGranted && notificationsGranted -> {
                    permissionDeniedMessage = null
                    showSettingsButton = false
                    val state = uiState
                    if (state.selectedLine != null && state.fromStation != null && state.toStation != null) {
                        viewModel.setTrackingActive(true)
                        onTripStarted(state.selectedLine.key, state.fromStation.name, state.toStation.name)
                    }
                }
                !locationGranted -> {
                    permissionDeniedMessage = "Location permission is required to track your trip"
                    showSettingsButton = false
                }
                !notificationsGranted -> {
                    permissionDeniedMessage = "Notification permission is required to alert you about your station. Please enable it in Settings."

                    var activityContext: Context = context
                    while (activityContext is ContextWrapper) {
                        if (activityContext is android.app.Activity) break
                        activityContext = activityContext.baseContext
                    }

                    val activity = activityContext as? android.app.Activity
                    if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val shouldShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
                        showSettingsButton = !shouldShowRationale
                    } else {
                        showSettingsButton = true
                    }
                }
            }
        }
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.isLoading) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Plan Trip", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(24.dp))

                // Line Selection
                var lineExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = lineExpanded,
                    onExpandedChange = { lineExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedLine?.name ?: "Select Line",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Metro Line") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lineExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = lineExpanded,
                        onDismissRequest = { lineExpanded = false }
                    ) {
                        uiState.allLines.forEach { line ->
                            DropdownMenuItem(
                                text = { Text(line.name) },
                                onClick = {
                                    viewModel.selectLine(line.key)
                                    lineExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // From Station Selection
                var fromExpanded by remember { mutableStateOf(false) }
                var fromSearchText by remember { mutableStateOf(uiState.fromStation?.name ?: "") }

                // Update search text if state changes externally
                LaunchedEffect(uiState.fromStation) {
                    fromSearchText = uiState.fromStation?.name ?: ""
                }

                ExposedDropdownMenuBox(
                    expanded = fromExpanded,
                    onExpandedChange = { fromExpanded = it }
                ) {
                    OutlinedTextField(
                        value = fromSearchText,
                        onValueChange = {
                            fromSearchText = it
                            fromExpanded = true
                        },
                        label = { Text("From Station") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = uiState.selectedLine != null,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                    )

                    val filteredFrom = uiState.stationsForSelectedLine.filter {
                        it.name.contains(fromSearchText, ignoreCase = true)
                    }

                    if (filteredFrom.isNotEmpty() && uiState.selectedLine != null) {
                        ExposedDropdownMenu(
                            expanded = fromExpanded,
                            onDismissRequest = { fromExpanded = false }
                        ) {
                            filteredFrom.forEach { station ->
                                DropdownMenuItem(
                                    text = { Text(station.name) },
                                    onClick = {
                                        viewModel.selectFromStation(station.name)
                                        fromExpanded = false
                                        fromSearchText = station.name
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // To Station Selection
                var toExpanded by remember { mutableStateOf(false) }
                var toSearchText by remember { mutableStateOf(uiState.toStation?.name ?: "") }

                // Update search text if state changes externally
                LaunchedEffect(uiState.toStation) {
                    toSearchText = uiState.toStation?.name ?: ""
                }

                ExposedDropdownMenuBox(
                    expanded = toExpanded,
                    onExpandedChange = { toExpanded = it }
                ) {
                    OutlinedTextField(
                        value = toSearchText,
                        onValueChange = {
                            toSearchText = it
                            toExpanded = true
                        },
                        label = { Text("To Station") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = uiState.selectedLine != null,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                    )

                    val filteredTo = uiState.stationsForSelectedLine.filter {
                        it.name.contains(toSearchText, ignoreCase = true) && it.name != uiState.fromStation?.name
                    }

                    if (filteredTo.isNotEmpty() && uiState.selectedLine != null) {
                        ExposedDropdownMenu(
                            expanded = toExpanded,
                            onDismissRequest = { toExpanded = false }
                        ) {
                            filteredTo.forEach { station ->
                                DropdownMenuItem(
                                    text = { Text(station.name) },
                                    onClick = {
                                        viewModel.selectToStation(station.name)
                                        toExpanded = false
                                        toSearchText = station.name
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))

                if (permissionDeniedMessage != null) {
                    Text(
                        text = permissionDeniedMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    if (showSettingsButton) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text("Open Settings")
                        }
                    }
                }

                if (!uiState.isTrackingActive) {
                    Button(
                        onClick = {
                            val locationGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                            } else {
                                true
                            }

                            if (locationGranted && notificationsGranted) {
                                val state = uiState
                                if (state.selectedLine != null && state.fromStation != null && state.toStation != null) {
                                    viewModel.setTrackingActive(true)
                                    onTripStarted(state.selectedLine.key, state.fromStation.name, state.toStation.name)
                                }
                            } else {
                                permissionLauncher.launch(permissionsToRequest)
                            }
                        },
                        enabled = viewModel.canStartTrip(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Trip")
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.setTrackingActive(false)
                            onTripStopped()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Stop Trip")
                    }
                }
            }
        }
    }
}

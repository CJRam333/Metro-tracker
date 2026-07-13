package com.example.hmrcompanion.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPlannerScreen(
    viewModel: TripPlannerViewModel,
    onTripStarted: (lineKey: String, fromStation: String, toStation: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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

                Button(
                    onClick = {
                        val state = uiState
                        if (state.selectedLine != null && state.fromStation != null && state.toStation != null) {
                            onTripStarted(state.selectedLine.key, state.fromStation.name, state.toStation.name)
                        }
                    },
                    enabled = viewModel.canStartTrip(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Trip")
                }
            }
        }
    }
}

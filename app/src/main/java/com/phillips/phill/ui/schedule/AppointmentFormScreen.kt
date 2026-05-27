package com.phillips.phill.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.phillips.phill.data.entity.VehicleEntity
import com.phillips.phill.domain.enums.AppointmentStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentFormScreen(
    appointmentId: String? = null,
    initialCustomerId: String? = null,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: AppointmentFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.initialize(appointmentId, initialCustomerId) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateEpochDay by remember { mutableStateOf<Long?>(null) }
    var selectedHour by remember { mutableStateOf(9) }
    var selectedMinute by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Appointment" else "New Appointment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.save(onSaveSuccess) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Save, contentDescription = "Save")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- Customer Search/Selection ---
            Text(
                "Customer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (state.selectedCustomer != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null)
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(
                                "${state.selectedCustomer!!.firstName} ${state.selectedCustomer!!.lastName}",
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                state.selectedCustomer!!.phoneNumber,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = {
                            // Clear selection to re-search
                            viewModel.searchCustomers("")
                        }) {
                            Text("Change")
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = state.customerSearchQuery,
                    onValueChange = { viewModel.searchCustomers(it) },
                    label = { Text("Search customers by name or phone") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (state.customerSearchResults.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            state.customerSearchResults.take(5).forEach { customer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectCustomer(customer) }
                                        .padding(12.dp)
                                ) {
                                    Text("${customer.firstName} ${customer.lastName}")
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        customer.phoneNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Vehicle Selection ---
            if (state.vehicles.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "Vehicle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.vehicles.forEach { vehicle ->
                        FilterChip(
                            selected = state.selectedVehicle?.id == vehicle.id,
                            onClick = { viewModel.selectVehicle(vehicle) },
                            label = {
                                Text(buildString {
                                    vehicle.year?.let { append("$it ") }
                                    append("${vehicle.make} ${vehicle.model}")
                                })
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.DirectionsCar,
                                    contentDescription = null,
                                    modifier = Modifier.height(16.dp)
                                )
                            }
                        )
                    }
                }
            }
            // Vehicle-not-selected warning
            if (state.selectedCustomer != null && state.selectedVehicle == null) {
                Text(
                    "⚠ No vehicle selected — a job will not be created for this appointment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // --- Date & Time ---
            Text(
                "Date & Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = if (selectedDateEpochDay != null) {
                        LocalDate.ofEpochDay(selectedDateEpochDay!!)
                            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    } else if (state.scheduledStartEpoch > 0) {
                        Instant.ofEpochMilli(state.scheduledStartEpoch)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    } else "",
                    onValueChange = {},
                    label = { Text("Date") },
                    modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Pick date")
                        }
                    }
                )

                OutlinedTextField(
                    value = if (state.scheduledStartEpoch > 0) {
                        Instant.ofEpochMilli(state.scheduledStartEpoch)
                            .atZone(ZoneId.systemDefault())
                            .toLocalTime()
                            .format(DateTimeFormatter.ofPattern("h:mm a"))
                    } else "",
                    onValueChange = {},
                    label = { Text("Time") },
                    modifier = Modifier.weight(1f).clickable { showTimePicker = true },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Pick time")
                        }
                    }
                )
            }

            if (state.scheduledStartEpoch > 0) {
                val setupTime = Instant.ofEpochMilli(state.scheduledStartEpoch - BUFFER_MILLIS)
                    .atZone(ZoneId.systemDefault()).toLocalTime()
                    .format(DateTimeFormatter.ofPattern("h:mm a"))
                Text(
                    "⏱ Arrive by $setupTime (15 min setup)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // --- Address ---
            OutlinedTextField(
                value = state.address,
                onValueChange = viewModel::updateAddress,
                label = { Text("Service Address *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2
            )

            // --- Status ---
            Text(
                "Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppointmentStatus.entries.forEach { status ->
                    FilterChip(
                        selected = state.status == status,
                        onClick = { viewModel.updateStatus(status) },
                        label = { Text(status.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // --- Notes ---
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 3
            )

            // Validation error
            state.validationError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // --- Date Picker Dialog ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDateEpochDay = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate().toEpochDay()
                        // Combine with current time selection
                        updateEpoch(selectedDateEpochDay!!, selectedHour, selectedMinute, viewModel)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- Time Picker Dialog ---
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Select Time", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            selectedHour = timePickerState.hour
                            selectedMinute = timePickerState.minute
                            selectedDateEpochDay?.let {
                                updateEpoch(it, selectedHour, selectedMinute, viewModel)
                            }
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

private fun updateEpoch(
    epochDay: Long,
    hour: Int,
    minute: Int,
    viewModel: AppointmentFormViewModel
) {
    val date = LocalDate.ofEpochDay(epochDay)
    val time = LocalTime.of(hour, minute)
    val epoch = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    viewModel.updateScheduledStartEpoch(epoch)
}

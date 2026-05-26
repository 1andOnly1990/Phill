package com.phillips.phill.ui.jobs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phillips.phill.domain.enums.JobStatus
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToInvoice: (String) -> Unit,
    onNavigateToCustomer: (String) -> Unit,
    viewModel: JobDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val job = state.job

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Detail") },
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
        }
    ) { paddingValues ->
        if (job == null) {
            if (!state.isLoading) {
                Text("Job not found", modifier = Modifier.padding(paddingValues).padding(16.dp))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Header: Customer + Vehicle ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    state.customer?.let { c ->
                        Text(
                            "${c.firstName} ${c.lastName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToCustomer(c.id) }
                        )
                        Text(c.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                        c.address?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    state.vehicle?.let { v ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                buildString {
                                    v.year?.let { append("$it ") }
                                    append("${v.make} ${v.model}")
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            // --- Status Controls ---
            val statusColor = when (job.status) {
                JobStatus.SCHEDULED -> Color.Gray
                JobStatus.EN_ROUTE -> Color(0xFF2196F3)
                JobStatus.ON_SITE -> Color(0xFFFF9800)
                JobStatus.COMPLETE -> Color(0xFF4CAF50)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = job.status.name.replace("_", " "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    if (job.status != JobStatus.COMPLETE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val nextLabel = when (job.status) {
                            JobStatus.SCHEDULED -> "Start Route"
                            JobStatus.EN_ROUTE -> "Arrived On Site"
                            JobStatus.ON_SITE -> "Mark Complete"
                            JobStatus.COMPLETE -> ""
                        }
                        Button(
                            onClick = { viewModel.advanceStatus() },
                            colors = ButtonDefaults.buttonColors(containerColor = statusColor)
                        ) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(nextLabel)
                        }
                    }
                }
            }

            // --- Time Clock ---
            HorizontalDivider()
            SectionHeader("Time Clock", Icons.Filled.Timer)

            // Live timer when clocked in
            val isClockedIn = state.activeClockEntry != null
            if (isClockedIn) {
                var elapsed by remember { mutableLongStateOf(0L) }
                LaunchedEffect(state.activeClockEntry) {
                    while (true) {
                        elapsed = (System.currentTimeMillis() - (state.activeClockEntry?.clockInEpoch ?: 0)) / 1000
                        delay(1000)
                    }
                }
                Text(
                    text = formatElapsed(elapsed),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (isClockedIn) {
                    Button(
                        onClick = { viewModel.clockOut() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clock Out")
                    }
                } else {
                    Button(
                        onClick = { viewModel.clockIn() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clock In")
                    }
                }
            }

            // Clock entries list
            if (state.clockEntries.isNotEmpty()) {
                val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                val zone = ZoneId.systemDefault()

                state.clockEntries.forEach { entry ->
                    val inTime = Instant.ofEpochMilli(entry.clockInEpoch).atZone(zone).toLocalTime()
                    val outTime = entry.clockOutEpoch?.let {
                        Instant.ofEpochMilli(it).atZone(zone).toLocalTime()
                    }
                    val duration = entry.clockOutEpoch?.let {
                        (it - entry.clockInEpoch) / 1000
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${inTime.format(timeFormatter)} → ${outTime?.format(timeFormatter) ?: "active"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        duration?.let {
                            Text(formatElapsed(it), style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Text(
                    "Total: ${formatElapsed(state.totalTimeSeconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // --- Mileage ---
            HorizontalDivider()
            SectionHeader("Mileage", Icons.Filled.Speed)

            state.mileageEntries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${String.format("%.1f", entry.miles)} mi",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    entry.purpose?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (state.mileageEntries.isNotEmpty()) {
                Text(
                    "Total: ${String.format("%.1f", state.totalMiles)} mi",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            OutlinedButton(
                onClick = { viewModel.showMileageDialog() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Log Mileage")
            }

            // --- Invoice Link ---
            HorizontalDivider()
            if (state.invoices.isNotEmpty()) {
                state.invoices.forEach { invoice ->
                    OutlinedButton(
                        onClick = { onNavigateToInvoice(invoice.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Receipt, contentDescription = null)
                        Text("  View Invoice (${invoice.status.name})")
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { onNavigateToInvoice(job.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Receipt, contentDescription = null)
                    Text("  Create Estimate")
                }
            }

            // Notes
            job.description?.let {
                HorizontalDivider()
                Text("Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // --- Mileage Dialog ---
    if (state.showMileageDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMileageDialog() },
            title = { Text("Log Mileage") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.mileageInput,
                        onValueChange = viewModel::updateMileageInput,
                        label = { Text("Miles driven") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.mileagePurpose,
                        onValueChange = viewModel::updateMileagePurpose,
                        label = { Text("Purpose (optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveMileage() }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissMileageDialog() }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

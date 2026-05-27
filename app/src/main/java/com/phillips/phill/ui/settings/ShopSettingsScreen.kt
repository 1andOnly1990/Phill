package com.phillips.phill.ui.settings

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ShopSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar("Settings saved")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shop Settings") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.save() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    if (state.saveSuccess) Icons.Filled.Check else Icons.Filled.Save,
                    contentDescription = "Save"
                )
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

            // --- Business Info ---
            SectionHeader("Business Information")

            OutlinedTextField(
                value = state.businessName,
                onValueChange = viewModel::updateBusinessName,
                label = { Text("Business Name") },
                placeholder = { Text("Phillips Mobile Automotive") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.businessAddress,
                onValueChange = viewModel::updateBusinessAddress,
                label = { Text("Business Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.ownerName,
                    onValueChange = viewModel::updateOwnerName,
                    label = { Text("Owner Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.ownerPhone,
                    onValueChange = viewModel::updateOwnerPhone,
                    label = { Text("Owner Phone") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Pricing ---
            SectionHeader("Pricing & Rates")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.laborRateDisplay,
                    onValueChange = viewModel::updateLaborRate,
                    label = { Text("Labor Rate ($/hr)") },
                    prefix = { Text("$") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = state.serviceFeeDisplay,
                    onValueChange = viewModel::updateServiceFee,
                    label = { Text("Service Fee") },
                    prefix = { Text("$") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.partsMarkupDisplay,
                    onValueChange = viewModel::updatePartsMarkup,
                    label = { Text("Parts Markup (%)") },
                    suffix = { Text("%") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("140% = 1.4× multiplier") }
                )
                OutlinedTextField(
                    value = state.taxRateDisplay,
                    onValueChange = viewModel::updateTaxRate,
                    label = { Text("Tax Rate (%)") },
                    suffix = { Text("%") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Parts only (SC §117-306)") }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Legal / ID ---
            SectionHeader("Legal & Identification")

            OutlinedTextField(
                value = state.taxId,
                onValueChange = viewModel::updateTaxId,
                label = { Text("Tax ID / EIN") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.licenseNumber,
                onValueChange = viewModel::updateLicenseNumber,
                label = { Text("Business License Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Auto-Reply ---
            SectionHeader("After-Hours Auto-Reply")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable auto-reply",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Automatically reply to texts received outside business hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = state.autoReplyEnabled,
                    onCheckedChange = viewModel::updateAutoReplyEnabled
                )
            }

            if (state.autoReplyEnabled) {
                OutlinedTextField(
                    value = state.autoReplyMessage,
                    onValueChange = viewModel::updateAutoReplyMessage,
                    label = { Text("Auto-Reply Message") },
                    placeholder = { Text("Thanks for reaching out! We're currently closed. We'll get back to you during business hours.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sent once per 2-hour window per contact")
                            Text(
                                text = "${state.autoReplyMessage.length}/160",
                                color = if (state.autoReplyMessage.length > 140)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    isError = state.autoReplyMessage.length >= 160
                )

                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Business Hours")
                Text(
                    text = "Texts received outside these hours will receive the auto-reply.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                state.businessHours.forEachIndexed { index, day ->
                    DayHoursRow(
                        dayName = day.dayName,
                        isOpen = day.isOpen,
                        opensAt = day.opensAt,
                        closesAt = day.closesAt,
                        onIsOpenChange = { viewModel.updateDayOpen(index, it) },
                        onOpensAtChange = { viewModel.updateDayOpensAt(index, it) },
                        onClosesAtChange = { viewModel.updateDayClosesAt(index, it) }
                    )
                    if (index < 6) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Room for FAB
        }
    }
}

@Composable
private fun DayHoursRow(
    dayName: String,
    isOpen: Boolean,
    opensAt: String,
    closesAt: String,
    onIsOpenChange: (Boolean) -> Unit,
    onOpensAtChange: (String) -> Unit,
    onClosesAtChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (isOpen) "Open" else "Closed",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOpen)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isOpen,
                onCheckedChange = onIsOpenChange
            )
        }
        if (isOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = opensAt,
                    onValueChange = onOpensAtChange,
                    label = { Text("Opens") },
                    placeholder = { Text("08:00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("24-hr HH:MM") }
                )
                OutlinedTextField(
                    value = closesAt,
                    onValueChange = onClosesAtChange,
                    label = { Text("Closes") },
                    placeholder = { Text("17:00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("24-hr HH:MM") }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

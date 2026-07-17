package com.phillips.phill.ui.comms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phillips.phill.data.entity.MessageEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    conversationId: String,
    onNavigateBack: () -> Unit,
    onCreateCustomer: (phone: String, firstName: String?, lastName: String?) -> Unit,
    onScheduleAppointment: (String) -> Unit,
    onAddVehicle: (customerId: String, year: String?, make: String?, model: String?) -> Unit,
    viewModel: ConversationDetailViewModel = hiltViewModel(key = conversationId)
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.initialize(conversationId) }
    val conversation = state.conversation
    val customer = state.customer
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // File picker for media sharing (Issue 5)
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && conversation != null) {
            viewModel.shareMedia(context, uri, conversation.phoneNumber)
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(conversation?.displayName ?: conversation?.phoneNumber ?: "Chat")
                        conversation?.phoneNumber?.let {
                            if (conversation.displayName != null) {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
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
        bottomBar = {
            // Compose bar — HITL Rule 1: operator types and taps Send
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach button (Issue 5)
                IconButton(
                    onClick = { filePickerLauncher.launch("*/*") }
                ) {
                    Icon(
                        Icons.Filled.AttachFile,
                        contentDescription = "Attach file",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = state.draftMessage,
                    onValueChange = viewModel::updateDraft,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message…") },
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = state.draftMessage.isNotBlank() && !state.isSending
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (state.draftMessage.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Context Header
            if (conversation != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (customer == null) {
                            Text(
                                text = "Unknown Lead",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    // Pre-fill with extracted name if available
                                    val extractedName = state.extractedData.names.firstOrNull()
                                    onCreateCustomer(
                                        conversation.phoneNumber,
                                        extractedName?.firstName,
                                        extractedName?.lastName
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Customer")
                            }
                        } else {
                            Text(
                                text = "${customer.firstName} ${customer.lastName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active Jobs: ${state.activeJobsCount}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onScheduleAppointment(customer.id) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Event, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Appt", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // Extraction chips — show detected data from inbound messages
            val extracted = state.extractedData
            if (extracted.vehicles.isNotEmpty() || extracted.symptoms.isNotEmpty() || extracted.appointmentIntent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    extracted.vehicles.forEach { vehicle ->
                        val label = listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model).joinToString(" ")
                        AssistChip(
                            onClick = {
                                val custId = customer?.id
                                if (custId != null) {
                                    onAddVehicle(
                                        custId,
                                        vehicle.year?.toString(),
                                        vehicle.make,
                                        vehicle.model
                                    )
                                } else {
                                    // No customer yet — create one first with extracted name
                                    val extractedName = extracted.names.firstOrNull()
                                    onCreateCustomer(
                                        conversation?.phoneNumber ?: "",
                                        extractedName?.firstName,
                                        extractedName?.lastName
                                    )
                                }
                            },
                            label = { Text(label, maxLines = 1) },
                            leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.padding(0.dp)) }
                        )
                    }
                    extracted.symptoms.forEach { symptom ->
                        AssistChip(
                            onClick = { viewModel.addSymptomNote(symptom.text) },
                            label = { Text(symptom.text, maxLines = 1) },
                            leadingIcon = { Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.padding(0.dp)) }
                        )
                    }
                    if (extracted.appointmentIntent) {
                        AssistChip(
                            onClick = {
                                val custId = customer?.id
                                if (custId != null) {
                                    onScheduleAppointment(custId)
                                } else {
                                    // Create customer first
                                    val extractedName = extracted.names.firstOrNull()
                                    onCreateCustomer(
                                        conversation?.phoneNumber ?: "",
                                        extractedName?.firstName,
                                        extractedName?.lastName
                                    )
                                }
                            },
                            label = { Text("Wants Appointment", maxLines = 1) },
                            leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.padding(0.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                    // Name chip — shows extracted names, tapping navigates to customer form
                    extracted.names.forEach { name ->
                        val nameLabel = listOfNotNull(name.firstName, name.lastName).joinToString(" ")
                        AssistChip(
                            onClick = {
                                if (customer == null) {
                                    onCreateCustomer(
                                        conversation?.phoneNumber ?: "",
                                        name.firstName,
                                        name.lastName
                                    )
                                }
                            },
                            label = { Text(nameLabel, maxLines = 1) },
                            leadingIcon = { Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.padding(0.dp)) },
                            colors = if (customer == null) AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) else AssistChipDefaults.assistChipColors()
                        )
                    }
                }
            }

            // Error bar
            state.sendError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp)
                )
            }

            // Messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageEntity) {
    val isOutbound = !message.isInbound
    val bubbleColor = if (isOutbound)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isOutbound)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val timeText = Instant.ofEpochMilli(message.timestampEpoch)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(timeFormatter)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutbound) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOutbound) 16.dp else 4.dp,
                        bottomEnd = if (isOutbound) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = message.body,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeText,
                    color = textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

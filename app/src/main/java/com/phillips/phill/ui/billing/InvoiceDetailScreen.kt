package com.phillips.phill.ui.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import android.content.Intent
import androidx.hilt.navigation.compose.hiltViewModel
import com.phillips.phill.domain.billing.BillingEngine
import com.phillips.phill.domain.enums.PaymentMethod
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: String,
    onNavigateBack: () -> Unit,
    viewModel: InvoiceDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.initialize(invoiceId) }
    val invoice = state.invoice

    // Handle PDF ready -> launch share intent
    LaunchedEffect(state.pdfReady) {
        if (state.pdfReady && state.pdfFile != null) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                state.pdfFile!!
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice ${invoice?.invoiceNumber ?: ""}")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Invoice"))
            viewModel.clearPdfState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice ${invoice?.status?.name ?: ""}") },
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
        if (invoice == null) {
            if (!state.isLoading) {
                Text("Invoice not found", modifier = Modifier.padding(paddingValues).padding(16.dp))
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
            // Totals card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleMedium)
                        Text(BillingEngine.formatCents(invoice.totalCents), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Paid", style = MaterialTheme.typography.bodyMedium)
                        Text(BillingEngine.formatCents(state.totalPaidCents), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Remaining", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            BillingEngine.formatCents(state.remainingCents),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.remainingCents > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Payments list
            if (state.payments.isNotEmpty()) {
                Text("Payment History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val timeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                val zone = ZoneId.systemDefault()

                state.payments.forEach { payment ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(BillingEngine.formatCents(payment.amountCents), fontWeight = FontWeight.Medium)
                                Text(
                                    "${payment.method.name} • ${Instant.ofEpochMilli(payment.paidAtEpoch).atZone(zone).format(timeFormatter)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                payment.referenceNumber?.let {
                                    Text("Ref: $it", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // Log payment button
            if (state.remainingCents > 0) {
                Button(
                    onClick = { viewModel.showPaymentDialog() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("  Log Payment")
                }
            }

            // Share / Preview buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.generatePdf(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Share Invoice")
                }
            }
        }
    }

    // Payment dialog
    if (state.showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPaymentDialog() },
            title = { Text("Log Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.paymentAmountDisplay,
                        onValueChange = viewModel::updatePaymentAmount,
                        label = { Text("Amount") },
                        prefix = { Text("$") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = state.paymentMethod == method,
                                onClick = { viewModel.updatePaymentMethod(method) },
                                label = { Text(method.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.paymentReference,
                        onValueChange = viewModel::updatePaymentReference,
                        label = { Text("Reference # (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.paymentNotes,
                        onValueChange = viewModel::updatePaymentNotes,
                        label = { Text("Notes (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.savePayment() }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { viewModel.dismissPaymentDialog() }) { Text("Cancel") } }
        )
    }
}

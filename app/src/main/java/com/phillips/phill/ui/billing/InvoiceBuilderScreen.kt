package com.phillips.phill.ui.billing

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phillips.phill.domain.billing.BillingEngine
import com.phillips.phill.domain.enums.InvoiceStatus
import com.phillips.phill.domain.enums.LineItemType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceBuilderScreen(
    jobId: String,
    invoiceId: String? = null,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: InvoiceBuilderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.initialize(jobId, invoiceId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state.status) {
                            InvoiceStatus.ESTIMATE -> "Estimate / Work Order"
                            InvoiceStatus.INVOICE -> "Invoice"
                            InvoiceStatus.PAID -> "Invoice (Paid)"
                            InvoiceStatus.VOID -> "Invoice (Void)"
                        }
                    )
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

            // --- Header ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(state.customerName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(state.vehicleDesc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // --- Line Items ---
            HorizontalDivider()
            Text("Line Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            state.lineItems.forEachIndexed { index, item ->
                LineItemCard(
                    item = item,
                    onUpdate = { updated -> viewModel.updateLineItem(index, updated) },
                    onRemove = { viewModel.removeLineItem(index) }
                )
            }

            // Add line item buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.addLineItem(LineItemType.LABOR) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(" Labor")
                }
                OutlinedButton(
                    onClick = { viewModel.addLineItem(LineItemType.PARTS) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(" Parts")
                }
                OutlinedButton(
                    onClick = { viewModel.addLineItem(LineItemType.MISC) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(" Misc")
                }
            }

            // --- Service Fee ---
            HorizontalDivider()
            OutlinedTextField(
                value = state.serviceFeeCentsDisplay,
                onValueChange = viewModel::updateServiceFee,
                label = { Text("Onsite Service Fee") },
                prefix = { Text("$") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // --- Totals ---
            HorizontalDivider()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TotalRow("Labor Subtotal", state.totals.laborSubtotalCents)
                    TotalRow("Parts Subtotal", state.totals.partsSubtotalCents)
                    if (state.totals.miscSubtotalCents > 0) {
                        TotalRow("Misc Subtotal", state.totals.miscSubtotalCents)
                    }
                    TotalRow("Service Fee", state.totals.serviceFeeCents)
                    TotalRow("Tax (${BillingEngine.formatBasisPoints(state.taxRateBasisPoints)} on parts)", state.totals.taxCents)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            BillingEngine.formatCents(state.totals.grandTotalCents),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // --- Legal Clause (editable) ---
            HorizontalDivider()
            Text("Terms & Conditions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = state.legalClause,
                onValueChange = viewModel::updateLegalClause,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            // --- Actions ---
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.saveAsEstimate(onSaveSuccess) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Text(" Save Estimate")
                }

                if (state.status == InvoiceStatus.ESTIMATE) {
                    Button(
                        onClick = { viewModel.finalizeAsInvoice(onSaveSuccess) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Finalize Invoice")
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun LineItemCard(
    item: LineItemUiModel,
    onUpdate: (LineItemUiModel) -> Unit,
    onRemove: () -> Unit
) {
    val typeColor = when (item.type) {
        LineItemType.LABOR -> Color(0xFF2196F3)
        LineItemType.PARTS -> Color(0xFFFF9800)
        LineItemType.MISC -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(item.type.name, style = MaterialTheme.typography.labelSmall) }
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }

            OutlinedTextField(
                value = item.description,
                onValueChange = { onUpdate(item.copy(description = it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = item.quantityDisplay,
                    onValueChange = { onUpdate(item.copy(quantityDisplay = it)) },
                    label = {
                        Text(
                            when (item.type) {
                                LineItemType.LABOR -> "Hours"
                                LineItemType.PARTS -> "Qty"
                                LineItemType.MISC -> "Qty"
                            }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = item.unitPriceDisplay,
                    onValueChange = { onUpdate(item.copy(unitPriceDisplay = it)) },
                    label = {
                        Text(
                            when (item.type) {
                                LineItemType.LABOR -> "$/hr"
                                LineItemType.PARTS -> "Cost (before markup)"
                                LineItemType.MISC -> "Amount"
                            }
                        )
                    },
                    prefix = { Text("$") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Show calculated total for this line
            val qty = BillingEngine.parseHoursToThousandths(item.quantityDisplay) ?: 0L
            val price = BillingEngine.parseDollarsToCents(item.unitPriceDisplay) ?: 0L
            val total = BillingEngine.calculateLineItemTotal(qty, price)
            val displayTotal = if (item.type == LineItemType.PARTS) {
                // Show marked-up price for parts
                val markup = 14000 // Will be overridden by actual value in ViewModel
                BillingEngine.formatCents(total)
            } else {
                BillingEngine.formatCents(total)
            }

            Text(
                "Line total: $displayTotal",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun TotalRow(label: String, cents: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(BillingEngine.formatCents(cents), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

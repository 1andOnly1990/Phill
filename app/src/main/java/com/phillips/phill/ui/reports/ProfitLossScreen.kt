package com.phillips.phill.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phillips.phill.domain.billing.BillingEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitLossScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfitLossViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profit & Loss") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val periodLabels = listOf("Month", "Quarter", "Year", "All Time")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                periodLabels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = state.selectedPeriod == index,
                        onClick = { viewModel.selectPeriod(index) },
                        label = { Text(label) }
                    )
                }
            }

            // Revenue
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Revenue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryRow(
                        "Total Revenue",
                        BillingEngine.formatCents(state.totalRevenueCents),
                        bold = true,
                        color = Color(0xFF4CAF50)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SummaryRow("Labor", BillingEngine.formatCents(state.laborRevenueCents))
                    SummaryRow("Parts (after markup)", BillingEngine.formatCents(state.partsRevenueCents))
                    SummaryRow("Misc", BillingEngine.formatCents(state.miscRevenueCents))
                    SummaryRow("Service Fees", BillingEngine.formatCents(state.serviceFeeCents))
                }
            }

            // Cost of Goods Sold
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Cost of Goods Sold",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryRow("Parts Cost", BillingEngine.formatCents(state.partsCostCents))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SummaryRow(
                        "Gross Profit",
                        BillingEngine.formatCents(state.grossProfitCents),
                        bold = true,
                        color = if (state.grossProfitCents >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            // Operating Expenses
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Operating Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryRow("Fuel", BillingEngine.formatCents(state.fuelExpensesCents))
                    SummaryRow("Supplies", BillingEngine.formatCents(state.suppliesExpensesCents))
                    SummaryRow("Tools", BillingEngine.formatCents(state.toolsExpensesCents))
                    SummaryRow("Other", BillingEngine.formatCents(state.otherExpensesCents))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SummaryRow(
                        "Total Expenses",
                        BillingEngine.formatCents(state.totalExpensesCents),
                        bold = true,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Mileage Deduction
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Mileage Deduction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryRow("Total Miles", "%.1f mi".format(state.totalMiles))
                    SummaryRow(
                        "IRS Deduction ($0.70/mi)",
                        BillingEngine.formatCents(state.mileageDeductionCents)
                    )
                }
            }

            // Bottom Line
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Bottom Line",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val netColor = if (state.netProfitCents >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    val prefix = if (state.netProfitCents < 0) "-" else ""
                    val displayCents = if (state.netProfitCents < 0) -state.netProfitCents else state.netProfitCents
                    Text(
                        "$prefix${BillingEngine.formatCents(displayCents)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = netColor
                    )
                    Text(
                        "Net Profit — ${state.periodLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    bold: Boolean = false,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

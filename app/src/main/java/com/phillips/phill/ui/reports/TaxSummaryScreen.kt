package com.phillips.phill.ui.reports

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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phillips.phill.domain.billing.BillingEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxSummaryScreen(
    onNavigateBack: () -> Unit,
    viewModel: TaxSummaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tax Summary") },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectYear(state.selectedYear - 1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous year")
                }
                Text(
                    "${state.selectedYear}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.selectYear(state.selectedYear + 1) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next year")
                }
            }

            val quarterLabels = listOf("Annual", "Q1", "Q2", "Q3", "Q4")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quarterLabels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = state.selectedQuarter == index,
                        onClick = { viewModel.selectQuarter(index) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                "Schedule C",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ScheduleCLine("Line 1", "Gross Receipts", state.grossReceiptsCents)
                    ScheduleCLine("Line 4", "Cost of Goods Sold (Parts)", state.partsCostCents)
                    ScheduleCLine(
                        "Line 7", "Gross Income", state.grossIncomeCents,
                        bold = true
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    ScheduleCLine(
                        "Line 9", "Car/Truck Expenses",
                        state.carExpensesCents,
                        subtitle = "%.1f mi × $${String.format("%.2f", state.mileageRateCents / 100.0)}/mi".format(state.totalMiles)
                    )
                    ScheduleCLine("Line 22", "Supplies", state.suppliesExpensesCents)
                    ScheduleCLine(
                        "Line 27a", "Other Expenses",
                        state.toolsExpensesCents + state.fuelExpensesCents + state.otherExpensesCents,
                        subtitle = buildOtherExpensesDetail(state)
                    )
                    ScheduleCLine("Line 28", "Total Expenses", state.totalExpensesCents, bold = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    val profitColor = if (state.netProfitCents >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    ScheduleCLine(
                        "Line 31", "Net Profit/Loss", state.netProfitCents,
                        bold = true, valueColor = profitColor
                    )
                }
            }

            Text(
                "Self-Employment Tax",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TaxLine("SE Tax (15.3%)", state.selfEmploymentTaxCents)
                    TaxLine("Quarterly Estimated Payment", state.quarterlyEstimatedPaymentCents)
                }
            }

            Text(
                "Mileage Detail",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MetricRow("Total Miles", "%.1f mi".format(state.totalMiles))
                    MetricRow(
                        "IRS Rate",
                        "$${String.format("%.2f", state.mileageRateCents / 100.0)}/mi"
                    )
                    MetricRow("Deduction", BillingEngine.formatCents(state.carExpensesCents))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun buildOtherExpensesDetail(state: TaxSummaryUiState): String {
    val parts = mutableListOf<String>()
    if (state.toolsExpensesCents > 0) parts.add("Tools ${BillingEngine.formatCents(state.toolsExpensesCents)}")
    if (state.fuelExpensesCents > 0) parts.add("Fuel ${BillingEngine.formatCents(state.fuelExpensesCents)}")
    if (state.otherExpensesCents > 0) parts.add("Other ${BillingEngine.formatCents(state.otherExpensesCents)}")
    return parts.joinToString(" · ")
}

@Composable
private fun ScheduleCLine(
    lineNumber: String,
    label: String,
    cents: Long,
    bold: Boolean = false,
    valueColor: Color = Color.Unspecified,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    lineNumber,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(52.dp)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 52.dp)
                )
            }
        }
        Text(
            BillingEngine.formatCents(cents),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}

@Composable
private fun TaxLine(label: String, cents: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            BillingEngine.formatCents(cents),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

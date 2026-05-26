package com.phillips.phill.ui.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    onJobClick: (String) -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
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
            // --- Mileage Summary ---
            SectionHeader(icon = Icons.Filled.DirectionsCar, title = "Mileage")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MetricRow("Today", "%.1f mi".format(state.todayMiles))
                    MetricRow("This Week", "%.1f mi".format(state.weekMiles))
                    MetricRow("This Month", "%.1f mi".format(state.monthMiles))
                }
            }

            // --- Revenue vs Expenses ---
            SectionHeader(icon = Icons.Filled.TrendingUp, title = "Revenue vs. Expenses")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FinancialRow("Revenue", state.todayRevenueCents, state.weekRevenueCents, state.monthRevenueCents, Color(0xFF4CAF50))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    FinancialRow("Expenses", state.todayExpensesCents, state.weekExpensesCents, state.monthExpensesCents, MaterialTheme.colorScheme.error)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    NetRow(
                        todayNet = state.todayRevenueCents - state.todayExpensesCents,
                        weekNet = state.weekRevenueCents - state.weekExpensesCents,
                        monthNet = state.monthRevenueCents - state.monthExpensesCents
                    )
                }
            }

            // --- Efficiency Metrics (ONLY if clock data exists) ---
            if (state.hasClockData) {
                SectionHeader(icon = Icons.Filled.Timer, title = "Efficiency")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val totalHours = state.totalActualSeconds / 3600.0
                        MetricRow("Total Hours Worked", "%.1f hrs".format(totalHours))
                        MetricRow("Completed Jobs", "${state.completedJobCount}")
                        if (state.completedJobCount > 0) {
                            val avgHours = totalHours / state.completedJobCount
                            MetricRow("Avg Hours/Job", "%.1f hrs".format(avgHours))
                        }
                    }
                }
            }

            // --- Job History ---
            HorizontalDivider()
            Text(
                "Job History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (state.completedJobs.isEmpty()) {
                Text(
                    "No completed jobs yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                val zone = ZoneId.systemDefault()

                state.completedJobs.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onJobClick(item.job.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.customerName, fontWeight = FontWeight.Medium)
                                item.job.description?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                            item.job.completedAtEpoch?.let { epoch ->
                                Text(
                                    Instant.ofEpochMilli(epoch)
                                        .atZone(zone)
                                        .toLocalDate()
                                        .format(dateFormatter),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
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

@Composable
private fun FinancialRow(label: String, today: Long, week: Long, month: Long, color: Color) {
    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FinancialCell("Today", today, color)
        FinancialCell("Week", week, color)
        FinancialCell("Month", month, color)
    }
}

@Composable
private fun FinancialCell(label: String, cents: Long, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(BillingEngine.formatCents(cents), fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NetRow(todayNet: Long, weekNet: Long, monthNet: Long) {
    Text("Net", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NetCell("Today", todayNet)
        NetCell("Week", weekNet)
        NetCell("Month", monthNet)
    }
}

@Composable
private fun NetCell(label: String, cents: Long) {
    val color = if (cents >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    val prefix = if (cents >= 0) "" else "-"
    val display = if (cents >= 0) BillingEngine.formatCents(cents) else BillingEngine.formatCents(-cents)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$prefix$display", fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.bodySmall)
    }
}

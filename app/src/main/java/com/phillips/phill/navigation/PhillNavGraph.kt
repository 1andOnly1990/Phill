package com.phillips.phill.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.phillips.phill.ui.components.PhillBottomBar

@Composable
fun PhillNavGraph() {
    val backStack = rememberNavBackStack(DashboardKey)

    Scaffold(
        bottomBar = {
            PhillBottomBar(
                backStack = backStack,
                onNavigate = { key ->
                    // Clear to root and navigate to new tab
                    while (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                    if (backStack.lastOrNull() != key) {
                        backStack.removeLastOrNull()
                        backStack.add(key)
                    }
                }
            )
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.padding(innerPadding),
            entryProvider = entryProvider {
                // --- Bottom bar tabs ---
                entry<DashboardKey> {
                    PlaceholderScreen("Dashboard")
                }
                entry<ScheduleKey> {
                    PlaceholderScreen("Schedule")
                }
                entry<JobQueueKey> {
                    PlaceholderScreen("Jobs")
                }
                entry<CommsKey> {
                    PlaceholderScreen("Communications")
                }

                // --- "More" menu screens ---
                entry<CustomerListKey> {
                    PlaceholderScreen("Customers")
                }
                entry<BillingKey> {
                    PlaceholderScreen("Billing")
                }
                entry<AnalyticsKey> {
                    PlaceholderScreen("Analytics")
                }
                entry<ShopSettingsKey> {
                    PlaceholderScreen("Shop Settings")
                }

                // --- Detail screens (placeholders until respective phases) ---
                entry<CustomerDetailKey> { key ->
                    PlaceholderScreen("Customer Detail: ${key.customerId}")
                }
                entry<CustomerFormKey> { key ->
                    PlaceholderScreen("Customer Form: ${key.customerId ?: "New"}")
                }
                entry<VehicleFormKey> { key ->
                    PlaceholderScreen("Vehicle Form: ${key.vehicleId ?: "New"}")
                }
                entry<AppointmentFormKey> { key ->
                    PlaceholderScreen("Appointment Form: ${key.appointmentId ?: "New"}")
                }
                entry<JobDetailKey> { key ->
                    PlaceholderScreen("Job Detail: ${key.jobId}")
                }
                entry<InvoiceBuilderKey> { key ->
                    PlaceholderScreen("Invoice Builder: ${key.invoiceId ?: "New"}")
                }
                entry<InvoiceDetailKey> { key ->
                    PlaceholderScreen("Invoice Detail: ${key.invoiceId}")
                }
                entry<ConversationKey> { key ->
                    PlaceholderScreen("Conversation: ${key.conversationId}")
                }
                entry<ExpenseFormKey> { key ->
                    PlaceholderScreen("Expense Form: ${key.expenseId ?: "New"}")
                }
                entry<PaymentLogKey> {
                    PlaceholderScreen("Payment Log")
                }
            }
        )
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name)
    }
}

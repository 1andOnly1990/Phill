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
import com.phillips.phill.ui.billing.ExpenseFormScreen
import com.phillips.phill.ui.billing.InvoiceBuilderScreen
import com.phillips.phill.ui.billing.InvoiceDetailScreen
import com.phillips.phill.ui.components.PhillBottomBar
import com.phillips.phill.ui.customers.CustomerDetailScreen
import com.phillips.phill.ui.customers.CustomerFormScreen
import com.phillips.phill.ui.customers.CustomerListScreen
import com.phillips.phill.ui.customers.VehicleFormScreen
import com.phillips.phill.ui.jobs.JobDetailScreen
import com.phillips.phill.ui.jobs.JobQueueScreen
import com.phillips.phill.ui.schedule.AppointmentFormScreen
import com.phillips.phill.ui.schedule.ScheduleScreen
import com.phillips.phill.ui.settings.ShopSettingsScreen

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
                    ScheduleScreen(
                        onAddAppointment = { backStack.add(AppointmentFormKey()) },
                        onAppointmentClick = { /* TODO: edit appointment */ }
                    )
                }
                entry<JobQueueKey> {
                    JobQueueScreen(
                        onJobClick = { jobId -> backStack.add(JobDetailKey(jobId)) }
                    )
                }
                entry<CommsKey> {
                    PlaceholderScreen("Communications")
                }

                // --- "More" menu screens ---
                entry<CustomerListKey> {
                    CustomerListScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onCustomerClick = { customerId ->
                            backStack.add(CustomerDetailKey(customerId))
                        },
                        onAddCustomer = {
                            backStack.add(CustomerFormKey())
                        }
                    )
                }
                entry<BillingKey> {
                    PlaceholderScreen("Billing")
                }
                entry<AnalyticsKey> {
                    PlaceholderScreen("Analytics")
                }
                entry<ShopSettingsKey> {
                    ShopSettingsScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }

                // --- Detail screens ---
                entry<CustomerDetailKey> { key ->
                    CustomerDetailScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onEditCustomer = { customerId ->
                            backStack.add(CustomerFormKey(customerId))
                        },
                        onAddVehicle = { customerId ->
                            backStack.add(VehicleFormKey(customerId))
                        },
                        onEditVehicle = { customerId, vehicleId ->
                            backStack.add(VehicleFormKey(customerId, vehicleId))
                        },
                        onViewJobs = { /* Phase 5 */ },
                        onViewInvoices = { /* Phase 6 */ }
                    )
                }
                entry<CustomerFormKey> { key ->
                    CustomerFormScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onSaveSuccess = { customerId ->
                            // Pop form and navigate to detail
                            backStack.removeLastOrNull()
                            backStack.add(CustomerDetailKey(customerId))
                        }
                    )
                }
                entry<VehicleFormKey> { key ->
                    VehicleFormScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onSaveSuccess = {
                            backStack.removeLastOrNull()
                        }
                    )
                }
                entry<AppointmentFormKey> { key ->
                    AppointmentFormScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onSaveSuccess = { backStack.removeLastOrNull() }
                    )
                }
                entry<JobDetailKey> { key ->
                    JobDetailScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onNavigateToInvoice = { id -> backStack.add(InvoiceBuilderKey(key.jobId, id)) },
                        onNavigateToCustomer = { customerId -> backStack.add(CustomerDetailKey(customerId)) }
                    )
                }
                entry<InvoiceBuilderKey> { key ->
                    InvoiceBuilderScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onSaveSuccess = { backStack.removeLastOrNull() }
                    )
                }
                entry<InvoiceDetailKey> { key ->
                    InvoiceDetailScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<ConversationKey> { key ->
                    PlaceholderScreen("Conversation: ${key.conversationId}")
                }
                entry<ExpenseFormKey> { key ->
                    ExpenseFormScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onSaveSuccess = { backStack.removeLastOrNull() }
                    )
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

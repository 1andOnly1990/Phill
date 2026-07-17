package com.phillips.phill.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.phillips.phill.ui.analytics.AnalyticsScreen
import com.phillips.phill.ui.billing.BillingHubScreen
import com.phillips.phill.ui.billing.ExpenseFormScreen
import com.phillips.phill.ui.billing.InvoiceBuilderScreen
import com.phillips.phill.ui.billing.InvoiceDetailScreen
import com.phillips.phill.ui.billing.PaymentLogScreen
import com.phillips.phill.ui.comms.ConversationDetailScreen
import com.phillips.phill.ui.comms.ConversationListScreen
import com.phillips.phill.ui.components.PhillBottomBar
import com.phillips.phill.ui.customers.CustomerDetailScreen
import com.phillips.phill.ui.customers.CustomerFormScreen
import com.phillips.phill.ui.customers.CustomerListScreen
import com.phillips.phill.ui.customers.VehicleFormScreen
import com.phillips.phill.ui.dashboard.DashboardScreen
import com.phillips.phill.ui.jobs.JobDetailScreen
import com.phillips.phill.ui.jobs.JobQueueScreen
import com.phillips.phill.ui.more.MoreHubScreen
import com.phillips.phill.ui.reports.ProfitLossScreen
import com.phillips.phill.ui.reports.TaxSummaryScreen
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
                    // Clear to root and navigate to new tab atomically
                    androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                        while (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                        if (backStack.lastOrNull() != key) {
                            backStack.removeLastOrNull()
                            backStack.add(key)
                        }
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
                    DashboardScreen()
                }
                entry<ScheduleKey> {
                    ScheduleScreen(
                        onAddAppointment = { backStack.add(AppointmentFormKey()) },
                        onAppointmentClick = { appointmentId -> backStack.add(AppointmentFormKey(appointmentId = appointmentId)) }
                    )
                }
                entry<JobQueueKey> {
                    JobQueueScreen(
                        onJobClick = { jobId -> backStack.add(JobDetailKey(jobId)) }
                    )
                }
                entry<CommsKey> {
                    ConversationListScreen(
                        onConversationClick = { convoId -> backStack.add(ConversationKey(convoId)) }
                    )
                }

                // --- "More" hub ---
                entry<MoreHubKey> {
                    MoreHubScreen(
                        onCustomers = { backStack.add(CustomerListKey) },
                        onBilling = { backStack.add(BillingKey) },
                        onAnalytics = { backStack.add(AnalyticsKey) },
                        onProfitLoss = { backStack.add(ProfitLossKey) },
                        onTaxSummary = { backStack.add(TaxSummaryKey) },
                        onSettings = { backStack.add(ShopSettingsKey) }
                    )
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
                    BillingHubScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onAddExpense = { backStack.add(ExpenseFormKey()) },
                        onViewPaymentLog = { backStack.add(PaymentLogKey) }
                    )
                }
                entry<AnalyticsKey> {
                    AnalyticsScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onJobClick = { jobId -> backStack.add(JobDetailKey(jobId)) }
                    )
                }
                entry<ShopSettingsKey> {
                    ShopSettingsScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<ProfitLossKey> {
                    ProfitLossScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<TaxSummaryKey> {
                    TaxSummaryScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }

                // --- Detail screens ---
                entry<CustomerDetailKey> { key ->
                    CustomerDetailScreen(
                        customerId = key.customerId,
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
                        customerId = key.customerId,
                        initialPhone = key.initialPhone,
                        initialFirstName = key.initialFirstName,
                        initialLastName = key.initialLastName,
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
                        customerId = key.customerId,
                        vehicleId = key.vehicleId,
                        initialYear = key.initialYear,
                        initialMake = key.initialMake,
                        initialModel = key.initialModel,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onSaveSuccess = {
                            backStack.removeLastOrNull()
                        }
                    )
                }
                entry<AppointmentFormKey> { key ->
                    AppointmentFormScreen(
                        appointmentId = key.appointmentId,
                        initialCustomerId = key.initialCustomerId,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onSaveSuccess = { backStack.removeLastOrNull() }
                    )
                }
                entry<JobDetailKey> { key ->
                    JobDetailScreen(
                        jobId = key.jobId,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onNavigateToInvoice = { id -> backStack.add(InvoiceBuilderKey(key.jobId, id)) },
                        onNavigateToCustomer = { customerId -> backStack.add(CustomerDetailKey(customerId)) }
                    )
                }
                entry<InvoiceBuilderKey> { key ->
                    InvoiceBuilderScreen(
                        jobId = key.jobId,
                        invoiceId = key.invoiceId,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onSaveSuccess = { backStack.removeLastOrNull() }
                    )
                }
                entry<InvoiceDetailKey> { key ->
                    InvoiceDetailScreen(
                        invoiceId = key.invoiceId,
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<ConversationKey> { key ->
                    ConversationDetailScreen(
                        conversationId = key.conversationId,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onCreateCustomer = { phone, firstName, lastName ->
                            backStack.add(CustomerFormKey(
                                initialPhone = phone,
                                initialFirstName = firstName,
                                initialLastName = lastName
                            ))
                        },
                        onScheduleAppointment = { customerId ->
                            backStack.add(AppointmentFormKey(initialCustomerId = customerId))
                        },
                        onAddVehicle = { customerId, year, make, model ->
                            backStack.add(VehicleFormKey(
                                customerId = customerId,
                                initialYear = year,
                                initialMake = make,
                                initialModel = model
                            ))
                        }
                    )
                }
                entry<ExpenseFormKey> { key ->
                    ExpenseFormScreen(
                        jobId = key.jobId,
                        expenseId = key.expenseId,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onSaveSuccess = { backStack.removeLastOrNull() }
                    )
                }
                entry<PaymentLogKey> {
                    PaymentLogScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }
            }
        )
    }
}


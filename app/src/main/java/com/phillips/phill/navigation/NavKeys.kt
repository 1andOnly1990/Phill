package com.phillips.phill.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// --- Primary bottom bar destinations ---
@Serializable data object DashboardKey : NavKey
@Serializable data object ScheduleKey : NavKey
@Serializable data object JobQueueKey : NavKey
@Serializable data object CommsKey : NavKey

// --- "More" menu destinations ---
@Serializable data object MoreHubKey : NavKey
@Serializable data object CustomerListKey : NavKey
@Serializable data object BillingKey : NavKey
@Serializable data object AnalyticsKey : NavKey
@Serializable data object ShopSettingsKey : NavKey
@Serializable data object ProfitLossKey : NavKey
@Serializable data object TaxSummaryKey : NavKey

// --- Detail destinations (Phase 3+) ---
@Serializable data class CustomerDetailKey(val customerId: String) : NavKey
@Serializable data class CustomerFormKey(val customerId: String? = null, val initialPhone: String? = null, val initialFirstName: String? = null, val initialLastName: String? = null) : NavKey
@Serializable data class VehicleFormKey(val customerId: String, val vehicleId: String? = null, val initialYear: String? = null, val initialMake: String? = null, val initialModel: String? = null) : NavKey
@Serializable data class AppointmentFormKey(val appointmentId: String? = null, val initialCustomerId: String? = null) : NavKey
@Serializable data class JobDetailKey(val jobId: String) : NavKey
@Serializable data class InvoiceBuilderKey(val jobId: String, val invoiceId: String? = null) : NavKey
@Serializable data class InvoiceDetailKey(val invoiceId: String) : NavKey
@Serializable data class ConversationKey(val conversationId: String) : NavKey
@Serializable data class ExpenseFormKey(val jobId: String? = null, val expenseId: String? = null) : NavKey
@Serializable data object PaymentLogKey : NavKey

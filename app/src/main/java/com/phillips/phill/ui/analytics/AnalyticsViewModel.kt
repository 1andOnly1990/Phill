package com.phillips.phill.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.JobEntity
import com.phillips.phill.data.repository.BillingRepository
import com.phillips.phill.data.repository.CustomerRepository
import com.phillips.phill.data.repository.JobRepository
import com.phillips.phill.data.repository.OperationsRepository
import com.phillips.phill.domain.billing.BillingEngine
import com.phillips.phill.domain.enums.JobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class JobWithCustomerName(
    val job: JobEntity,
    val customerName: String
)

data class AnalyticsUiState(
    val todayMiles: Double = 0.0,
    val weekMiles: Double = 0.0,
    val monthMiles: Double = 0.0,
    val todayRevenueCents: Long = 0L,
    val weekRevenueCents: Long = 0L,
    val monthRevenueCents: Long = 0L,
    val todayExpensesCents: Long = 0L,
    val weekExpensesCents: Long = 0L,
    val monthExpensesCents: Long = 0L,
    val completedJobs: List<JobWithCustomerName> = emptyList(),
    val totalActualSeconds: Long = 0L,
    val completedJobCount: Int = 0,
    val hasClockData: Boolean = false,
    val mileageDeductionCents: Long = 0L,
    val mileageRateCentsPerMile: Int = 70,
    val isLoading: Boolean = true
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val billingRepository: BillingRepository,
    private val operationsRepository: OperationsRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            val startOfWeek = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()

            val monthStart = today.withDayOfMonth(1)
            val startOfMonth = monthStart.atStartOfDay(zone).toInstant().toEpochMilli()

            combine(
                jobRepository.observeAllJobs(),
                billingRepository.observeAllPayments(),
                operationsRepository.observeAllExpenses(),
                operationsRepository.observeAllMileage(),
                jobRepository.observeAllClockEntries()
            ) { allJobs, allPayments, allExpenses, allMileage, allClockEntries ->

                // --- Mileage ---
                val todayMiles = allMileage
                    .filter { it.recordedAtEpoch in startOfDay..endOfDay }
                    .sumOf { it.miles }
                val weekMiles = allMileage
                    .filter { it.recordedAtEpoch in startOfWeek..endOfDay }
                    .sumOf { it.miles }
                val monthMiles = allMileage
                    .filter { it.recordedAtEpoch in startOfMonth..endOfDay }
                    .sumOf { it.miles }

                val mileageDeduction = BillingEngine.calculateMileageDeduction(monthMiles)

                // --- Revenue ---
                val todayRevenue = allPayments
                    .filter { it.paidAtEpoch in startOfDay..endOfDay }
                    .sumOf { it.amountCents }
                val weekRevenue = allPayments
                    .filter { it.paidAtEpoch in startOfWeek..endOfDay }
                    .sumOf { it.amountCents }
                val monthRevenue = allPayments
                    .filter { it.paidAtEpoch in startOfMonth..endOfDay }
                    .sumOf { it.amountCents }

                // --- Expenses ---
                val todayExpenses = allExpenses
                    .filter { it.dateEpoch in startOfDay..endOfDay }
                    .sumOf { it.amountCents }
                val weekExpenses = allExpenses
                    .filter { it.dateEpoch in startOfWeek..endOfDay }
                    .sumOf { it.amountCents }
                val monthExpenses = allExpenses
                    .filter { it.dateEpoch in startOfMonth..endOfDay }
                    .sumOf { it.amountCents }

                // --- Completed Jobs ---
                val completedJobEntities = allJobs
                    .filter { it.status == JobStatus.COMPLETE }
                    .sortedByDescending { it.completedAtEpoch }

                // --- Clock / Efficiency ---
                val totalSeconds = allClockEntries
                    .filter { it.clockOutEpoch != null }
                    .sumOf { (it.clockOutEpoch!! - it.clockInEpoch) / 1000 }
                val hasClockData = allClockEntries.any { it.clockOutEpoch != null }

                AnalyticsUiState(
                    todayMiles = todayMiles,
                    weekMiles = weekMiles,
                    monthMiles = monthMiles,
                    todayRevenueCents = todayRevenue,
                    weekRevenueCents = weekRevenue,
                    monthRevenueCents = monthRevenue,
                    todayExpensesCents = todayExpenses,
                    weekExpensesCents = weekExpenses,
                    monthExpensesCents = monthExpenses,
                    completedJobs = emptyList(), // populated below
                    totalActualSeconds = totalSeconds,
                    completedJobCount = completedJobEntities.size,
                    hasClockData = hasClockData,
                    mileageDeductionCents = mileageDeduction,
                    mileageRateCentsPerMile = BillingEngine.IRS_MILEAGE_RATE_CENTS_2025,
                    isLoading = false
                ) to completedJobEntities
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value to emptyList())
                .collect { (state, completedJobEntities) ->
                    // Resolve customer names off the main combine path
                    val jobsWithNames = completedJobEntities.map { job ->
                        val customer = customerRepository.getCustomerById(job.customerId)
                        JobWithCustomerName(
                            job = job,
                            customerName = customer?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown"
                        )
                    }
                    _uiState.value = state.copy(completedJobs = jobsWithNames)
                }
        }
    }
}

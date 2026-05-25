package com.phillips.phill.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.AppointmentEntity
import com.phillips.phill.data.entity.JobEntity
import com.phillips.phill.data.repository.BillingRepository
import com.phillips.phill.data.repository.JobRepository
import com.phillips.phill.data.repository.OperationsRepository
import com.phillips.phill.data.repository.ScheduleRepository
import com.phillips.phill.domain.enums.InvoiceStatus
import com.phillips.phill.domain.enums.JobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DashboardUiState(
    val todayJobs: List<JobEntity> = emptyList(),
    val activeJobCount: Int = 0,
    val todayAppointments: List<AppointmentEntity> = emptyList(),
    val unpaidInvoiceCount: Int = 0,
    val outstandingBalanceCents: Long = 0L,
    val todayRevenueCents: Long = 0L,
    val todayMileage: Double = 0.0,
    val totalCustomers: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val scheduleRepository: ScheduleRepository,
    private val billingRepository: BillingRepository,
    private val operationsRepository: OperationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

            combine(
                jobRepository.observeAllJobs(),
                scheduleRepository.observeByDateRange(startOfDay, endOfDay),
                billingRepository.observeInvoicesByStatus(InvoiceStatus.INVOICE),
                billingRepository.observeAllPayments()
            ) { allJobs, todayAppts, unpaidInvoices, allPayments ->
                val activeJobs = allJobs.filter { it.status != JobStatus.COMPLETE }
                val todayJobs = allJobs.filter { job ->
                    job.createdAtEpoch in startOfDay..endOfDay ||
                        job.status != JobStatus.COMPLETE
                }

                val outstandingBalance = unpaidInvoices.sumOf { it.totalCents }
                val todayPayments = allPayments.filter { it.paidAtEpoch in startOfDay..endOfDay }
                val todayRevenue = todayPayments.sumOf { it.amountCents }

                DashboardUiState(
                    todayJobs = todayJobs,
                    activeJobCount = activeJobs.size,
                    todayAppointments = todayAppts,
                    unpaidInvoiceCount = unpaidInvoices.size,
                    outstandingBalanceCents = outstandingBalance,
                    todayRevenueCents = todayRevenue,
                    isLoading = false
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value)
                .collect { _uiState.value = it }
        }
    }
}

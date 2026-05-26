package com.phillips.phill.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.ExpenseEntity
import com.phillips.phill.data.entity.InvoiceEntity
import com.phillips.phill.data.repository.BillingRepository
import com.phillips.phill.data.repository.OperationsRepository
import com.phillips.phill.domain.enums.InvoiceStatus
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

data class BillingHubUiState(
    val todayRevenueCents: Long = 0L,
    val weekRevenueCents: Long = 0L,
    val monthRevenueCents: Long = 0L,
    val unpaidInvoices: List<InvoiceEntity> = emptyList(),
    val outstandingTotalCents: Long = 0L,
    val recentExpenses: List<ExpenseEntity> = emptyList(),
    val totalExpensesTodayCents: Long = 0L,
    val isLoading: Boolean = true
)

@HiltViewModel
class BillingHubViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val operationsRepository: OperationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingHubUiState())
    val uiState: StateFlow<BillingHubUiState> = _uiState.asStateFlow()

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
                billingRepository.observeAllPayments(),
                billingRepository.observeInvoicesByStatus(InvoiceStatus.INVOICE),
                operationsRepository.observeAllExpenses()
            ) { allPayments, unpaidInvoices, allExpenses ->

                val todayRevenue = allPayments
                    .filter { it.paidAtEpoch in startOfDay..endOfDay }
                    .sumOf { it.amountCents }
                val weekRevenue = allPayments
                    .filter { it.paidAtEpoch in startOfWeek..endOfDay }
                    .sumOf { it.amountCents }
                val monthRevenue = allPayments
                    .filter { it.paidAtEpoch in startOfMonth..endOfDay }
                    .sumOf { it.amountCents }

                val outstanding = unpaidInvoices.sumOf { it.totalCents }

                val todayExpenses = allExpenses
                    .filter { it.dateEpoch in startOfDay..endOfDay }
                    .sumOf { it.amountCents }

                val recent = allExpenses
                    .sortedByDescending { it.dateEpoch }
                    .take(10)

                BillingHubUiState(
                    todayRevenueCents = todayRevenue,
                    weekRevenueCents = weekRevenue,
                    monthRevenueCents = monthRevenue,
                    unpaidInvoices = unpaidInvoices,
                    outstandingTotalCents = outstanding,
                    recentExpenses = recent,
                    totalExpensesTodayCents = todayExpenses,
                    isLoading = false
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value)
                .collect { _uiState.value = it }
        }
    }
}

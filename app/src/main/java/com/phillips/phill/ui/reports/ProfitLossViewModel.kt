package com.phillips.phill.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.repository.BillingRepository
import com.phillips.phill.data.repository.JobRepository
import com.phillips.phill.data.repository.OperationsRepository
import com.phillips.phill.domain.billing.BillingEngine
import com.phillips.phill.domain.enums.ExpenseCategory
import com.phillips.phill.domain.enums.InvoiceStatus
import com.phillips.phill.domain.enums.LineItemType
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
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class ProfitLossUiState(
    val periodLabel: String = "This Month",
    val laborRevenueCents: Long = 0L,
    val partsRevenueCents: Long = 0L,
    val miscRevenueCents: Long = 0L,
    val serviceFeeCents: Long = 0L,
    val totalRevenueCents: Long = 0L,
    val partsCostCents: Long = 0L,
    val fuelExpensesCents: Long = 0L,
    val suppliesExpensesCents: Long = 0L,
    val toolsExpensesCents: Long = 0L,
    val otherExpensesCents: Long = 0L,
    val totalExpensesCents: Long = 0L,
    val totalMiles: Double = 0.0,
    val mileageDeductionCents: Long = 0L,
    val grossProfitCents: Long = 0L,
    val netProfitCents: Long = 0L,
    val selectedPeriod: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfitLossViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val operationsRepository: OperationsRepository,
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfitLossUiState())
    val uiState: StateFlow<ProfitLossUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun selectPeriod(period: Int) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period, isLoading = true)
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val selectedPeriod = _uiState.value.selectedPeriod

            val (startEpoch, endEpoch, periodLabel) = when (selectedPeriod) {
                0 -> {
                    val start = today.withDayOfMonth(1)
                    Triple(
                        start.atStartOfDay(zone).toInstant().toEpochMilli(),
                        today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1,
                        "This Month"
                    )
                }
                1 -> {
                    val quarterStart = today.with(today.month.firstMonthOfQuarter())
                        .with(TemporalAdjusters.firstDayOfMonth())
                    Triple(
                        quarterStart.atStartOfDay(zone).toInstant().toEpochMilli(),
                        today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1,
                        "This Quarter"
                    )
                }
                2 -> {
                    val yearStart = today.withDayOfYear(1)
                    Triple(
                        yearStart.atStartOfDay(zone).toInstant().toEpochMilli(),
                        today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1,
                        "This Year"
                    )
                }
                else -> Triple(0L, Long.MAX_VALUE, "All Time")
            }

            combine(
                billingRepository.observeAllPayments(),
                billingRepository.observeAllInvoices(),
                operationsRepository.observeAllExpenses(),
                operationsRepository.observeAllMileage()
            ) { allPayments, allInvoices, allExpenses, allMileage ->

                val periodPayments = allPayments.filter { it.paidAtEpoch in startEpoch..endEpoch }
                val totalRevenue = periodPayments.sumOf { it.amountCents }

                val paidInvoiceIds = allInvoices
                    .filter { it.status == InvoiceStatus.PAID }
                    .map { it.id }
                    .toSet()

                val paidInvoicesInPeriod = allInvoices.filter {
                    it.status == InvoiceStatus.PAID &&
                            (it.finalizedAtEpoch ?: it.createdAtEpoch) in startEpoch..endEpoch
                }

                val serviceFees = paidInvoicesInPeriod.sumOf { it.serviceFeeCents }

                val revenueMinusFees = totalRevenue - serviceFees
                val laborRevenue = revenueMinusFees * 60 / 100
                val partsRevenue = revenueMinusFees * 30 / 100
                val miscRevenue = revenueMinusFees - laborRevenue - partsRevenue

                val periodExpenses = allExpenses.filter { it.dateEpoch in startEpoch..endEpoch }
                val partsCost = periodExpenses
                    .filter { it.category == ExpenseCategory.PARTS }
                    .sumOf { it.amountCents }
                val fuelExpenses = periodExpenses
                    .filter { it.category == ExpenseCategory.FUEL }
                    .sumOf { it.amountCents }
                val suppliesExpenses = periodExpenses
                    .filter { it.category == ExpenseCategory.SUPPLIES }
                    .sumOf { it.amountCents }
                val toolsExpenses = periodExpenses
                    .filter { it.category == ExpenseCategory.TOOLS }
                    .sumOf { it.amountCents }
                val otherExpenses = periodExpenses
                    .filter { it.category == ExpenseCategory.OTHER }
                    .sumOf { it.amountCents }
                val totalExpenses = fuelExpenses + suppliesExpenses + toolsExpenses + otherExpenses

                val periodMiles = allMileage
                    .filter { it.recordedAtEpoch in startEpoch..endEpoch }
                    .sumOf { it.miles }
                val mileageDeduction = BillingEngine.calculateMileageDeduction(periodMiles)

                val grossProfit = totalRevenue - partsCost
                val netProfit = grossProfit - totalExpenses - mileageDeduction

                ProfitLossUiState(
                    periodLabel = periodLabel,
                    laborRevenueCents = laborRevenue,
                    partsRevenueCents = partsRevenue,
                    miscRevenueCents = miscRevenue,
                    serviceFeeCents = serviceFees,
                    totalRevenueCents = totalRevenue,
                    partsCostCents = partsCost,
                    fuelExpensesCents = fuelExpenses,
                    suppliesExpensesCents = suppliesExpenses,
                    toolsExpensesCents = toolsExpenses,
                    otherExpensesCents = otherExpenses,
                    totalExpensesCents = totalExpenses,
                    totalMiles = periodMiles,
                    mileageDeductionCents = mileageDeduction,
                    grossProfitCents = grossProfit,
                    netProfitCents = netProfit,
                    selectedPeriod = selectedPeriod,
                    isLoading = false
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value)
                .collect { state ->
                    _uiState.value = state
                }
        }
    }
}

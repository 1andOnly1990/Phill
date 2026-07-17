package com.phillips.phill.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.repository.BillingRepository
import com.phillips.phill.data.repository.OperationsRepository
import com.phillips.phill.domain.billing.BillingEngine
import com.phillips.phill.domain.enums.ExpenseCategory
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

data class TaxSummaryUiState(
    val selectedYear: Int = LocalDate.now().year,
    val selectedQuarter: Int = 0,
    val grossReceiptsCents: Long = 0L,
    val partsCostCents: Long = 0L,
    val grossIncomeCents: Long = 0L,
    val carExpensesCents: Long = 0L,
    val suppliesExpensesCents: Long = 0L,
    val toolsExpensesCents: Long = 0L,
    val fuelExpensesCents: Long = 0L,
    val otherExpensesCents: Long = 0L,
    val totalExpensesCents: Long = 0L,
    val netProfitCents: Long = 0L,
    val selfEmploymentTaxCents: Long = 0L,
    val quarterlyEstimatedPaymentCents: Long = 0L,
    val totalMiles: Double = 0.0,
    val mileageRateCents: Int = BillingEngine.IRS_MILEAGE_RATE_CENTS_2025,
    val isLoading: Boolean = true
)

@HiltViewModel
class TaxSummaryViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val operationsRepository: OperationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaxSummaryUiState())
    val uiState: StateFlow<TaxSummaryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun selectYear(year: Int) {
        _uiState.value = _uiState.value.copy(selectedYear = year, isLoading = true)
        loadData()
    }

    fun selectQuarter(quarter: Int) {
        _uiState.value = _uiState.value.copy(selectedQuarter = quarter, isLoading = true)
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val year = _uiState.value.selectedYear
            val quarter = _uiState.value.selectedQuarter

            val (rangeStart, rangeEnd) = calculateDateRange(year, quarter, zone)

            combine(
                billingRepository.observeAllPayments(),
                operationsRepository.observeAllExpenses(),
                operationsRepository.observeAllMileage()
            ) { allPayments, allExpenses, allMileage ->

                val periodPayments = allPayments.filter { it.paidAtEpoch in rangeStart..rangeEnd }
                val periodExpenses = allExpenses.filter { it.dateEpoch in rangeStart..rangeEnd }
                val periodMileage = allMileage.filter { it.recordedAtEpoch in rangeStart..rangeEnd }

                val grossReceipts = periodPayments.sumOf { it.amountCents }

                var partsCost = 0L
                var fuelExpenses = 0L
                var suppliesExpenses = 0L
                var toolsExpenses = 0L
                var otherExpenses = 0L

                for (expense in periodExpenses) {
                    when (expense.category) {
                        ExpenseCategory.PARTS -> partsCost += expense.amountCents
                        ExpenseCategory.FUEL -> fuelExpenses += expense.amountCents
                        ExpenseCategory.SUPPLIES -> suppliesExpenses += expense.amountCents
                        ExpenseCategory.TOOLS -> toolsExpenses += expense.amountCents
                        ExpenseCategory.OTHER -> otherExpenses += expense.amountCents
                    }
                }

                val grossIncome = grossReceipts - partsCost

                val totalMiles = periodMileage.sumOf { it.miles }
                val carExpenses = BillingEngine.calculateMileageDeduction(totalMiles)

                val totalExpenses = carExpenses + suppliesExpenses + toolsExpenses + fuelExpenses + otherExpenses
                val netProfit = grossIncome - totalExpenses

                val selfEmploymentTax = if (netProfit > 0) {
                    netProfit * 9235 / 10000 * 1530 / 10000
                } else {
                    0L
                }

                val quarterlyEstimated = selfEmploymentTax / 4

                TaxSummaryUiState(
                    selectedYear = year,
                    selectedQuarter = quarter,
                    grossReceiptsCents = grossReceipts,
                    partsCostCents = partsCost,
                    grossIncomeCents = grossIncome,
                    carExpensesCents = carExpenses,
                    suppliesExpensesCents = suppliesExpenses,
                    toolsExpensesCents = toolsExpenses,
                    fuelExpensesCents = fuelExpenses,
                    otherExpensesCents = otherExpenses,
                    totalExpensesCents = totalExpenses,
                    netProfitCents = netProfit,
                    selfEmploymentTaxCents = selfEmploymentTax,
                    quarterlyEstimatedPaymentCents = quarterlyEstimated,
                    totalMiles = totalMiles,
                    mileageRateCents = BillingEngine.IRS_MILEAGE_RATE_CENTS_2025,
                    isLoading = false
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value)
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    private fun calculateDateRange(year: Int, quarter: Int, zone: ZoneId): Pair<Long, Long> {
        val startDate = when (quarter) {
            1 -> LocalDate.of(year, 1, 1)
            2 -> LocalDate.of(year, 4, 1)
            3 -> LocalDate.of(year, 7, 1)
            4 -> LocalDate.of(year, 10, 1)
            else -> LocalDate.of(year, 1, 1)
        }
        val endDate = when (quarter) {
            1 -> LocalDate.of(year, 4, 1)
            2 -> LocalDate.of(year, 7, 1)
            3 -> LocalDate.of(year, 10, 1)
            4 -> LocalDate.of(year + 1, 1, 1)
            else -> LocalDate.of(year + 1, 1, 1)
        }
        val startEpoch = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endEpoch = endDate.atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return startEpoch to endEpoch
    }
}

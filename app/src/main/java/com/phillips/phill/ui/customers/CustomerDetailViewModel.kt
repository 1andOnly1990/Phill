package com.phillips.phill.ui.customers


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.CustomerEntity
import com.phillips.phill.data.entity.InvoiceEntity
import com.phillips.phill.data.entity.JobEntity
import com.phillips.phill.data.entity.VehicleEntity
import com.phillips.phill.data.repository.BillingRepository
import com.phillips.phill.data.repository.CustomerRepository
import com.phillips.phill.data.repository.JobRepository
import com.phillips.phill.domain.enums.InvoiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerDetailUiState(
    val customer: CustomerEntity? = null,
    val vehicles: List<VehicleEntity> = emptyList(),
    val jobs: List<JobEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val outstandingBalanceCents: Long = 0L,
    val currentAgingCents: Long = 0L,
    val aging30Cents: Long = 0L,
    val aging60Cents: Long = 0L,
    val aging90PlusCents: Long = 0L,
    val unpaidInvoices: List<InvoiceEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val jobRepository: JobRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private var customerId: String = ""
    private var initialized = false

    private val _uiState = MutableStateFlow(CustomerDetailUiState())
    val uiState: StateFlow<CustomerDetailUiState> = _uiState.asStateFlow()

    fun initialize(customerId: String) {
        if (initialized) return
        initialized = true
        this.customerId = customerId
        loadCustomer()
    }

    private fun loadCustomer() {
        viewModelScope.launch {
            val customer = customerRepository.getCustomerById(customerId)
            if (customer == null) {
                _uiState.value = CustomerDetailUiState(isLoading = false)
                return@launch
            }

            // Observe vehicles, jobs, and invoices reactively
            combine(
                customerRepository.observeVehicles(customerId),
                jobRepository.observeByCustomer(customerId),
                billingRepository.observeInvoicesByCustomer(customerId)
            ) { vehicles, jobs, invoices ->
                val unpaid = invoices.filter { it.status == InvoiceStatus.INVOICE }
                val outstandingBalance = unpaid.sumOf { it.totalCents }

                // Aging buckets based on finalization date
                val now = System.currentTimeMillis()
                val day30 = 30L * 24 * 60 * 60 * 1000
                val day60 = 60L * 24 * 60 * 60 * 1000
                val day90 = 90L * 24 * 60 * 60 * 1000

                var current = 0L
                var over30 = 0L
                var over60 = 0L
                var over90 = 0L
                for (inv in unpaid) {
                    val age = now - (inv.finalizedAtEpoch ?: inv.createdAtEpoch)
                    when {
                        age >= day90 -> over90 += inv.totalCents
                        age >= day60 -> over60 += inv.totalCents
                        age >= day30 -> over30 += inv.totalCents
                        else -> current += inv.totalCents
                    }
                }

                CustomerDetailUiState(
                    customer = customer,
                    vehicles = vehicles,
                    jobs = jobs,
                    invoices = invoices,
                    outstandingBalanceCents = outstandingBalance,
                    currentAgingCents = current,
                    aging30Cents = over30,
                    aging60Cents = over60,
                    aging90PlusCents = over90,
                    unpaidInvoices = unpaid,
                    isLoading = false
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value)
                .collect { _uiState.value = it }
        }
    }

    fun deleteCustomer(onComplete: () -> Unit) {
        viewModelScope.launch {
            val customer = _uiState.value.customer ?: return@launch
            customerRepository.deleteCustomer(customer)
            onComplete()
        }
    }
}

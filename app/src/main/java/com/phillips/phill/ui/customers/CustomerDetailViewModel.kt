package com.phillips.phill.ui.customers

import androidx.lifecycle.SavedStateHandle
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
    val isLoading: Boolean = true
)

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val customerRepository: CustomerRepository,
    private val jobRepository: JobRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val customerId: String = savedStateHandle.get<String>("customerId") ?: ""

    private val _uiState = MutableStateFlow(CustomerDetailUiState())
    val uiState: StateFlow<CustomerDetailUiState> = _uiState.asStateFlow()

    init {
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
                val outstandingBalance = invoices
                    .filter { it.status == InvoiceStatus.INVOICE }
                    .sumOf { it.totalCents }

                CustomerDetailUiState(
                    customer = customer,
                    vehicles = vehicles,
                    jobs = jobs,
                    invoices = invoices,
                    outstandingBalanceCents = outstandingBalance,
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

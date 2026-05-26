package com.phillips.phill.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.PaymentEntity
import com.phillips.phill.data.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentLogUiState(
    val payments: List<PaymentEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PaymentLogViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentLogUiState())
    val uiState: StateFlow<PaymentLogUiState> = _uiState.asStateFlow()

    init {
        loadPayments()
    }

    private fun loadPayments() {
        viewModelScope.launch {
            billingRepository.observeAllPayments()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
                .collect { payments ->
                    _uiState.value = PaymentLogUiState(
                        payments = payments,
                        isLoading = false
                    )
                }
        }
    }
}

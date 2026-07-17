package com.phillips.phill.ui.customers


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.CustomerEntity
import com.phillips.phill.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerFormUiState(
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val phoneError: String? = null,
    val nameError: String? = null
)

@HiltViewModel
class CustomerFormViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private var customerId: String? = null
    private var initialPhone: String? = null
    private var initialized = false

    private val _uiState = MutableStateFlow(CustomerFormUiState())
    val uiState: StateFlow<CustomerFormUiState> = _uiState.asStateFlow()

    private var existingCustomer: CustomerEntity? = null

    fun initialize(
        customerId: String?,
        initialPhone: String?,
        initialFirstName: String? = null,
        initialLastName: String? = null
    ) {
        if (initialized) return
        initialized = true
        this.customerId = customerId
        this.initialPhone = initialPhone

        // Pre-fill from extraction data
        val prefilled = _uiState.value.copy(
            phoneNumber = initialPhone ?: _uiState.value.phoneNumber,
            firstName = initialFirstName ?: _uiState.value.firstName,
            lastName = initialLastName ?: _uiState.value.lastName
        )
        _uiState.value = prefilled

        if (customerId != null) {
            loadExisting(customerId)
        }
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            val customer = customerRepository.getCustomerById(id)
            if (customer != null) {
                existingCustomer = customer
                _uiState.value = CustomerFormUiState(
                    firstName = customer.firstName,
                    lastName = customer.lastName,
                    phoneNumber = customer.phoneNumber,
                    email = customer.email ?: "",
                    address = customer.address ?: "",
                    notes = customer.notes ?: "",
                    isEditMode = true
                )
            }
        }
    }

    fun updateFirstName(value: String) {
        _uiState.value = _uiState.value.copy(firstName = value, nameError = null)
    }

    fun updateLastName(value: String) {
        _uiState.value = _uiState.value.copy(lastName = value, nameError = null)
    }

    fun updatePhoneNumber(value: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = value, phoneError = null)
    }

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun updateAddress(value: String) {
        _uiState.value = _uiState.value.copy(address = value)
    }

    fun updateNotes(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun save(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return // Guard check to prevent double clicks

        // Validation per plan: first name, last name, phone required
        if (state.firstName.isBlank() || state.lastName.isBlank()) {
            _uiState.value = state.copy(nameError = "First and last name are required")
            return
        }
        if (state.phoneNumber.isBlank()) {
            _uiState.value = state.copy(phoneError = "Phone number is required")
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            // Check for duplicate phone (unique constraint)
            val phoneOwner = customerRepository.getCustomerByPhone(state.phoneNumber)
            if (phoneOwner != null && phoneOwner.id != (existingCustomer?.id ?: "")) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    phoneError = "Phone number already in use by ${phoneOwner.firstName} ${phoneOwner.lastName}"
                )
                return@launch
            }

            val customer = if (existingCustomer != null) {
                existingCustomer!!.copy(
                    firstName = state.firstName.trim(),
                    lastName = state.lastName.trim(),
                    phoneNumber = state.phoneNumber.trim(),
                    email = state.email.trim().ifBlank { null },
                    address = state.address.trim().ifBlank { null },
                    notes = state.notes.trim().ifBlank { null },
                    updatedAtEpoch = System.currentTimeMillis()
                )
            } else {
                CustomerEntity(
                    firstName = state.firstName.trim(),
                    lastName = state.lastName.trim(),
                    phoneNumber = state.phoneNumber.trim(),
                    email = state.email.trim().ifBlank { null },
                    address = state.address.trim().ifBlank { null },
                    notes = state.notes.trim().ifBlank { null }
                )
            }

            customerRepository.saveCustomer(customer)
            _uiState.value = _uiState.value.copy(isSaving = false)
            onSuccess(customer.id)
        }
    }
}

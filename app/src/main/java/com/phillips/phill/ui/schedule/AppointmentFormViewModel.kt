package com.phillips.phill.ui.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.AppointmentEntity
import com.phillips.phill.data.entity.CustomerEntity
import com.phillips.phill.data.entity.JobEntity
import com.phillips.phill.data.entity.VehicleEntity
import com.phillips.phill.data.repository.CustomerRepository
import com.phillips.phill.data.repository.JobRepository
import com.phillips.phill.data.repository.ScheduleRepository
import com.phillips.phill.domain.enums.AppointmentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppointmentFormUiState(
    val selectedCustomer: CustomerEntity? = null,
    val selectedVehicle: VehicleEntity? = null,
    val address: String = "",
    val scheduledDate: String = "",
    val scheduledTime: String = "",
    val scheduledStartEpoch: Long = 0L,
    val scheduledEndEpoch: Long? = null,
    val notes: String = "",
    val status: AppointmentStatus = AppointmentStatus.PENDING,
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val customerSearchQuery: String = "",
    val customerSearchResults: List<CustomerEntity> = emptyList(),
    val vehicles: List<VehicleEntity> = emptyList(),
    val validationError: String? = null
)

@HiltViewModel
class AppointmentFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleRepository: ScheduleRepository,
    private val customerRepository: CustomerRepository,
    private val jobRepository: JobRepository
) : ViewModel() {

    private val appointmentId: String? = savedStateHandle.get<String>("appointmentId")

    private val _uiState = MutableStateFlow(AppointmentFormUiState())
    val uiState: StateFlow<AppointmentFormUiState> = _uiState.asStateFlow()

    private var existingAppointment: AppointmentEntity? = null

    init {
        if (appointmentId != null) {
            loadExisting(appointmentId)
        }
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            val appt = scheduleRepository.getById(id) ?: return@launch
            existingAppointment = appt
            val customer = customerRepository.getCustomerById(appt.customerId)
            val vehicle = appt.vehicleId?.let { customerRepository.getVehicleById(it) }

            _uiState.value = _uiState.value.copy(
                selectedCustomer = customer,
                selectedVehicle = vehicle,
                address = appt.address,
                scheduledStartEpoch = appt.scheduledStartEpoch,
                scheduledEndEpoch = appt.scheduledEndEpoch,
                notes = appt.notes ?: "",
                status = appt.status,
                isEditMode = true
            )
            // Load vehicles for selected customer
            customer?.let { loadVehiclesForCustomer(it.id) }
        }
    }

    fun searchCustomers(query: String) {
        _uiState.value = _uiState.value.copy(customerSearchQuery = query)
        viewModelScope.launch {
            customerRepository.searchCustomers(query)
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                .collect { results ->
                    _uiState.value = _uiState.value.copy(customerSearchResults = results)
                }
        }
    }

    fun selectCustomer(customer: CustomerEntity) {
        _uiState.value = _uiState.value.copy(
            selectedCustomer = customer,
            customerSearchQuery = "",
            customerSearchResults = emptyList(),
            // Pre-fill address from customer (HITL Rule 4: editable)
            address = customer.address ?: _uiState.value.address,
            selectedVehicle = null
        )
        loadVehiclesForCustomer(customer.id)
    }

    private fun loadVehiclesForCustomer(customerId: String) {
        viewModelScope.launch {
            customerRepository.observeVehicles(customerId)
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                .collect { vehicles ->
                    _uiState.value = _uiState.value.copy(vehicles = vehicles)
                    // Auto-select if only one vehicle
                    if (vehicles.size == 1 && _uiState.value.selectedVehicle == null) {
                        _uiState.value = _uiState.value.copy(selectedVehicle = vehicles[0])
                    }
                }
        }
    }

    fun selectVehicle(vehicle: VehicleEntity) {
        _uiState.value = _uiState.value.copy(selectedVehicle = vehicle)
    }

    fun updateAddress(value: String) {
        _uiState.value = _uiState.value.copy(address = value, validationError = null)
    }

    fun updateNotes(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun updateScheduledStartEpoch(epoch: Long) {
        _uiState.value = _uiState.value.copy(scheduledStartEpoch = epoch, validationError = null)
    }

    fun updateStatus(status: AppointmentStatus) {
        _uiState.value = _uiState.value.copy(status = status)
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value

        if (state.selectedCustomer == null) {
            _uiState.value = state.copy(validationError = "Please select a customer")
            return
        }
        if (state.address.isBlank()) {
            _uiState.value = state.copy(validationError = "Address is required")
            return
        }
        if (state.scheduledStartEpoch == 0L) {
            _uiState.value = state.copy(validationError = "Please select a date and time")
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val appointment = if (existingAppointment != null) {
                existingAppointment!!.copy(
                    customerId = state.selectedCustomer.id,
                    vehicleId = state.selectedVehicle?.id,
                    address = state.address.trim(),
                    scheduledStartEpoch = state.scheduledStartEpoch,
                    scheduledEndEpoch = state.scheduledEndEpoch,
                    notes = state.notes.trim().ifBlank { null },
                    status = state.status
                )
            } else {
                AppointmentEntity(
                    customerId = state.selectedCustomer.id,
                    vehicleId = state.selectedVehicle?.id,
                    address = state.address.trim(),
                    scheduledStartEpoch = state.scheduledStartEpoch,
                    scheduledEndEpoch = state.scheduledEndEpoch,
                    notes = state.notes.trim().ifBlank { null },
                    status = state.status
                )
            }

            scheduleRepository.saveAppointment(appointment)

            // Per plan line 561: creating appointment auto-creates a Job in SCHEDULED status
            if (existingAppointment == null && state.selectedVehicle != null) {
                val job = JobEntity(
                    customerId = state.selectedCustomer.id,
                    vehicleId = state.selectedVehicle.id,
                    appointmentId = appointment.id,
                    description = state.notes.trim().ifBlank { null }
                )
                jobRepository.saveJob(job)

                // Link job back to appointment
                scheduleRepository.saveAppointment(appointment.copy(jobId = job.id))
            }

            _uiState.value = _uiState.value.copy(isSaving = false)
            onSuccess()
        }
    }
}

package com.phillips.phill.ui.jobs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.ClockEntryEntity
import com.phillips.phill.data.entity.CustomerEntity
import com.phillips.phill.data.entity.InvoiceEntity
import com.phillips.phill.data.entity.JobEntity
import com.phillips.phill.data.entity.MileageEntryEntity
import com.phillips.phill.data.entity.VehicleEntity
import com.phillips.phill.data.repository.BillingRepository
import com.phillips.phill.data.repository.CustomerRepository
import com.phillips.phill.data.repository.JobRepository
import com.phillips.phill.data.repository.OperationsRepository
import com.phillips.phill.domain.enums.JobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobDetailUiState(
    val job: JobEntity? = null,
    val customer: CustomerEntity? = null,
    val vehicle: VehicleEntity? = null,
    val clockEntries: List<ClockEntryEntity> = emptyList(),
    val activeClockEntry: ClockEntryEntity? = null,
    val mileageEntries: List<MileageEntryEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val totalTimeSeconds: Long = 0L,
    val totalMiles: Double = 0.0,
    val isLoading: Boolean = true,
    // Mileage form
    val showMileageDialog: Boolean = false,
    val mileageInput: String = "",
    val mileagePurpose: String = ""
)

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jobRepository: JobRepository,
    private val customerRepository: CustomerRepository,
    private val billingRepository: BillingRepository,
    private val operationsRepository: OperationsRepository
) : ViewModel() {

    private val jobId: String = savedStateHandle.get<String>("jobId") ?: ""

    private val _uiState = MutableStateFlow(JobDetailUiState())
    val uiState: StateFlow<JobDetailUiState> = _uiState.asStateFlow()

    init {
        loadJobDetail()
    }

    private fun loadJobDetail() {
        viewModelScope.launch {
            val job = jobRepository.getJobById(jobId)
            if (job == null) {
                _uiState.value = JobDetailUiState(isLoading = false)
                return@launch
            }

            val customer = customerRepository.getCustomerById(job.customerId)
            val vehicle = customerRepository.getVehicleById(job.vehicleId)

            // Reactive streams for clock, mileage, invoices
            combine(
                jobRepository.observeClockEntries(jobId),
                operationsRepository.observeMileageByJob(jobId),
                billingRepository.observeInvoicesByJob(jobId)
            ) { clock, mileage, invoices ->
                val totalSeconds = clock.sumOf { entry ->
                    val end = entry.clockOutEpoch ?: System.currentTimeMillis()
                    (end - entry.clockInEpoch) / 1000L
                }
                val active = clock.find { it.clockOutEpoch == null }
                val totalMiles = mileage.sumOf { it.miles }

                JobDetailUiState(
                    job = job,
                    customer = customer,
                    vehicle = vehicle,
                    clockEntries = clock,
                    activeClockEntry = active,
                    mileageEntries = mileage,
                    invoices = invoices,
                    totalTimeSeconds = totalSeconds,
                    totalMiles = totalMiles,
                    isLoading = false
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value)
                .collect { _uiState.value = it }
        }
    }

    // --- Status progression (cannot go backwards) ---
    fun advanceStatus() {
        val currentStatus = _uiState.value.job?.status ?: return
        val nextStatus = when (currentStatus) {
            JobStatus.SCHEDULED -> JobStatus.EN_ROUTE
            JobStatus.EN_ROUTE -> JobStatus.ON_SITE
            JobStatus.ON_SITE -> JobStatus.COMPLETE
            JobStatus.COMPLETE -> return // Already complete, no further advancement
        }
        viewModelScope.launch {
            jobRepository.updateJobStatus(jobId, nextStatus)
            // Reload job to get updated status
            val updated = jobRepository.getJobById(jobId)
            _uiState.value = _uiState.value.copy(job = updated)
        }
    }

    // --- Clock operations ---
    fun clockIn() {
        viewModelScope.launch {
            jobRepository.clockIn(jobId)
        }
    }

    fun clockOut() {
        val entry = _uiState.value.activeClockEntry ?: return
        viewModelScope.launch {
            jobRepository.clockOut(entry.id)
        }
    }

    // --- Mileage ---
    fun showMileageDialog() {
        _uiState.value = _uiState.value.copy(showMileageDialog = true, mileageInput = "", mileagePurpose = "")
    }

    fun dismissMileageDialog() {
        _uiState.value = _uiState.value.copy(showMileageDialog = false)
    }

    fun updateMileageInput(value: String) {
        _uiState.value = _uiState.value.copy(mileageInput = value)
    }

    fun updateMileagePurpose(value: String) {
        _uiState.value = _uiState.value.copy(mileagePurpose = value)
    }

    fun saveMileage() {
        val miles = _uiState.value.mileageInput.toDoubleOrNull() ?: return

        viewModelScope.launch {
            val entry = MileageEntryEntity(
                jobId = jobId,
                miles = miles,
                purpose = _uiState.value.mileagePurpose.trim().ifBlank { null },
                recordedAtEpoch = System.currentTimeMillis()
            )
            operationsRepository.saveMileage(entry)
            _uiState.value = _uiState.value.copy(showMileageDialog = false)
        }
    }
}

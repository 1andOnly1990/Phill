package com.phillips.phill.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.CustomerEntity
import com.phillips.phill.data.entity.JobEntity
import com.phillips.phill.data.entity.VehicleEntity
import com.phillips.phill.data.repository.CustomerRepository
import com.phillips.phill.data.repository.JobRepository
import com.phillips.phill.domain.enums.JobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobWithContext(
    val job: JobEntity,
    val customerName: String = "",
    val vehicleDesc: String = ""
)

@HiltViewModel
class JobQueueViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _activeJobs = MutableStateFlow<List<JobWithContext>>(emptyList())
    val activeJobs: StateFlow<List<JobWithContext>> = _activeJobs.asStateFlow()

    private val _completedJobs = MutableStateFlow<List<JobWithContext>>(emptyList())
    val completedJobs: StateFlow<List<JobWithContext>> = _completedJobs.asStateFlow()

    private val _customerCache = mutableMapOf<String, CustomerEntity>()
    private val _vehicleCache = mutableMapOf<String, VehicleEntity>()

    init {
        loadJobs()
    }

    private fun loadJobs() {
        viewModelScope.launch {
            jobRepository.observeAllJobs()
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                .collect { jobs ->
                    val active = mutableListOf<JobWithContext>()
                    val completed = mutableListOf<JobWithContext>()

                    for (job in jobs) {
                        val context = buildJobContext(job)
                        if (job.status == JobStatus.COMPLETE) {
                            completed.add(context)
                        } else {
                            active.add(context)
                        }
                    }

                    // Sort active: EN_ROUTE first, then ON_SITE, then SCHEDULED
                    _activeJobs.value = active.sortedBy {
                        when (it.job.status) {
                            JobStatus.ON_SITE -> 0
                            JobStatus.EN_ROUTE -> 1
                            JobStatus.SCHEDULED -> 2
                            JobStatus.COMPLETE -> 3
                        }
                    }
                    _completedJobs.value = completed.sortedByDescending { it.job.completedAtEpoch }
                }
        }
    }

    private suspend fun buildJobContext(job: JobEntity): JobWithContext {
        val customer = _customerCache.getOrPut(job.customerId) {
            customerRepository.getCustomerById(job.customerId) ?: CustomerEntity(
                firstName = "Unknown", lastName = "", phoneNumber = ""
            )
        }
        val vehicle = _vehicleCache.getOrPut(job.vehicleId) {
            customerRepository.getVehicleById(job.vehicleId) ?: VehicleEntity(
                customerId = "", make = "Unknown", model = ""
            )
        }

        val vehicleDesc = buildString {
            vehicle.year?.let { append("$it ") }
            append("${vehicle.make} ${vehicle.model}")
        }

        return JobWithContext(
            job = job,
            customerName = "${customer.firstName} ${customer.lastName}",
            vehicleDesc = vehicleDesc
        )
    }

    fun updateStatus(jobId: String, newStatus: JobStatus) {
        viewModelScope.launch {
            jobRepository.updateJobStatus(jobId, newStatus)
        }
    }
}

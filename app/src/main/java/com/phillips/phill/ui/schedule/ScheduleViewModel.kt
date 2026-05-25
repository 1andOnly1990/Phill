package com.phillips.phill.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.AppointmentEntity
import com.phillips.phill.data.entity.CustomerEntity
import com.phillips.phill.data.repository.CustomerRepository
import com.phillips.phill.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class ScheduleViewMode { DAY, WEEK, MONTH }

/** 15 minutes in millis — added before and after each appointment for setup/cleanup */
const val BUFFER_MILLIS = 15L * 60L * 1000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _viewMode = MutableStateFlow(ScheduleViewMode.DAY)
    val viewMode: StateFlow<ScheduleViewMode> = _viewMode.asStateFlow()

    private val _customerCache = mutableMapOf<String, CustomerEntity>()

    /**
     * Appointments for the current view window (day/week/month).
     * Queried reactively whenever selectedDate or viewMode changes.
     */
    val appointments: StateFlow<List<AppointmentEntity>> = combine(
        _selectedDate, _viewMode
    ) { date, mode -> Pair(date, mode) }
        .flatMapLatest { (date, mode) ->
            val zone = ZoneId.systemDefault()
            val (start, end) = when (mode) {
                ScheduleViewMode.DAY -> {
                    val s = date.atStartOfDay(zone).toInstant().toEpochMilli()
                    val e = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    s to e
                }
                ScheduleViewMode.WEEK -> {
                    val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                    val s = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
                    val e = weekStart.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    s to e
                }
                ScheduleViewMode.MONTH -> {
                    val monthStart = date.withDayOfMonth(1)
                    val s = monthStart.atStartOfDay(zone).toInstant().toEpochMilli()
                    val e = monthStart.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    s to e
                }
            }
            scheduleRepository.observeByDateRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Navigation ---

    fun setViewMode(mode: ScheduleViewMode) {
        _viewMode.value = mode
    }

    fun navigateToDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun previous() {
        _selectedDate.value = when (_viewMode.value) {
            ScheduleViewMode.DAY -> _selectedDate.value.minusDays(1)
            ScheduleViewMode.WEEK -> _selectedDate.value.minusWeeks(1)
            ScheduleViewMode.MONTH -> _selectedDate.value.minusMonths(1)
        }
    }

    fun next() {
        _selectedDate.value = when (_viewMode.value) {
            ScheduleViewMode.DAY -> _selectedDate.value.plusDays(1)
            ScheduleViewMode.WEEK -> _selectedDate.value.plusWeeks(1)
            ScheduleViewMode.MONTH -> _selectedDate.value.plusMonths(1)
        }
    }

    fun today() {
        _selectedDate.value = LocalDate.now()
    }

    // --- Display helpers ---

    /**
     * Returns the effective start epoch with 15-min setup buffer subtracted.
     * The operator should arrive 15 min before the appointment.
     */
    fun getBufferedStartEpoch(appointment: AppointmentEntity): Long {
        return appointment.scheduledStartEpoch - BUFFER_MILLIS
    }

    /**
     * Returns the effective end epoch with 15-min cleanup buffer added.
     * If no end time is set, defaults to 1hr after start + buffer.
     */
    fun getBufferedEndEpoch(appointment: AppointmentEntity): Long {
        val baseEnd = appointment.scheduledEndEpoch
            ?: (appointment.scheduledStartEpoch + 60L * 60L * 1000L) // default 1hr
        return baseEnd + BUFFER_MILLIS
    }

    suspend fun getCustomerName(customerId: String): String {
        _customerCache[customerId]?.let {
            return "${it.firstName} ${it.lastName}"
        }
        val customer = customerRepository.getCustomerById(customerId)
        if (customer != null) {
            _customerCache[customerId] = customer
            return "${customer.firstName} ${customer.lastName}"
        }
        return "Unknown"
    }
}

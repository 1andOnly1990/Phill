package com.phillips.phill.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.ShopProfileEntity
import com.phillips.phill.data.repository.OperationsRepository
import com.phillips.phill.domain.billing.BillingEngine
import com.phillips.phill.domain.model.BusinessHoursSchedule
import com.phillips.phill.domain.model.DaySchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Holds the editable state for one day's hours in the UI. */
data class DayScheduleState(
    val dayName: String,
    val isOpen: Boolean,
    val opensAt: String,   // 24-hour format "HH:MM", e.g. "08:00"
    val closesAt: String   // 24-hour format "HH:MM", e.g. "17:00"
)

private fun defaultBusinessHours(): List<DayScheduleState> = listOf(
    DayScheduleState("Sunday",    false, "08:00", "17:00"),
    DayScheduleState("Monday",    true,  "08:00", "17:00"),
    DayScheduleState("Tuesday",   true,  "08:00", "17:00"),
    DayScheduleState("Wednesday", true,  "08:00", "17:00"),
    DayScheduleState("Thursday",  true,  "08:00", "17:00"),
    DayScheduleState("Friday",    true,  "08:00", "17:00"),
    DayScheduleState("Saturday",  false, "08:00", "12:00")
)

private val DAY_NAMES = listOf(
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
)

data class ShopSettingsUiState(
    val businessName: String = "",
    val businessAddress: String = "",
    val ownerName: String = "",
    val ownerPhone: String = "",
    val laborRateDisplay: String = "125.00",
    val serviceFeeDisplay: String = "60.00",
    val partsMarkupDisplay: String = "140.00",
    val taxRateDisplay: String = "6.00",
    val taxId: String = "",
    val licenseNumber: String = "",
    // --- Auto-reply ---
    val autoReplyEnabled: Boolean = false,
    val autoReplyMessage: String = "",
    val businessHours: List<DayScheduleState> = defaultBusinessHours(),
    // --- Meta ---
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isLoaded: Boolean = false
)

@HiltViewModel
class ShopSettingsViewModel @Inject constructor(
    private val operationsRepository: OperationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopSettingsUiState())
    val uiState: StateFlow<ShopSettingsUiState> = _uiState.asStateFlow()

    private var currentProfileId: Int = 1

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            var profile = operationsRepository.getShopProfile()
            if (profile == null) {
                profile = ShopProfileEntity()
                operationsRepository.saveShopProfile(profile)
            }
            currentProfileId = profile.id

            // Parse business hours from JSON, or use defaults
            val schedule = profile.businessHoursJson?.let { json ->
                try { Json.decodeFromString<BusinessHoursSchedule>(json) }
                catch (e: Exception) { BusinessHoursSchedule() }
            } ?: BusinessHoursSchedule()

            val businessHours = schedule.days.mapIndexed { index, day ->
                DayScheduleState(
                    dayName = DAY_NAMES.getOrElse(index) { "Day $index" },
                    isOpen = day.isOpen,
                    opensAt = day.openHHMM,
                    closesAt = day.closeHHMM
                )
            }

            _uiState.value = ShopSettingsUiState(
                businessName = profile.businessName ?: "",
                businessAddress = profile.businessAddress ?: "",
                ownerName = profile.ownerName ?: "",
                ownerPhone = profile.ownerPhone ?: "",
                laborRateDisplay = formatCentsToInput(profile.laborRateCents),
                serviceFeeDisplay = formatCentsToInput(profile.serviceFeeCents),
                partsMarkupDisplay = formatBasisPointsToInput(profile.partsMarkupBasisPoints),
                taxRateDisplay = formatBasisPointsToInput(profile.taxRateBasisPoints),
                taxId = profile.taxId ?: "",
                licenseNumber = profile.licenseNumber ?: "",
                autoReplyEnabled = profile.autoReplyEnabled,
                autoReplyMessage = profile.autoReplyMessage ?: "",
                businessHours = if (businessHours.size == 7) businessHours else defaultBusinessHours(),
                isLoaded = true
            )
        }
    }

    // --- Existing field update methods ---

    fun updateBusinessName(value: String) {
        _uiState.value = _uiState.value.copy(businessName = value, saveSuccess = false)
    }

    fun updateBusinessAddress(value: String) {
        _uiState.value = _uiState.value.copy(businessAddress = value, saveSuccess = false)
    }

    fun updateOwnerName(value: String) {
        _uiState.value = _uiState.value.copy(ownerName = value, saveSuccess = false)
    }

    fun updateOwnerPhone(value: String) {
        _uiState.value = _uiState.value.copy(ownerPhone = value, saveSuccess = false)
    }

    fun updateLaborRate(value: String) {
        _uiState.value = _uiState.value.copy(laborRateDisplay = value, saveSuccess = false)
    }

    fun updateServiceFee(value: String) {
        _uiState.value = _uiState.value.copy(serviceFeeDisplay = value, saveSuccess = false)
    }

    fun updatePartsMarkup(value: String) {
        _uiState.value = _uiState.value.copy(partsMarkupDisplay = value, saveSuccess = false)
    }

    fun updateTaxRate(value: String) {
        _uiState.value = _uiState.value.copy(taxRateDisplay = value, saveSuccess = false)
    }

    fun updateTaxId(value: String) {
        _uiState.value = _uiState.value.copy(taxId = value, saveSuccess = false)
    }

    fun updateLicenseNumber(value: String) {
        _uiState.value = _uiState.value.copy(licenseNumber = value, saveSuccess = false)
    }

    // --- New auto-reply field update methods ---

    fun updateAutoReplyEnabled(value: Boolean) {
        _uiState.value = _uiState.value.copy(autoReplyEnabled = value, saveSuccess = false)
    }

    /** Clamps message to 160 characters (single SMS segment). */
    fun updateAutoReplyMessage(value: String) {
        _uiState.value = _uiState.value.copy(
            autoReplyMessage = value.take(160),
            saveSuccess = false
        )
    }

    fun updateDayOpen(dayIndex: Int, isOpen: Boolean) {
        val updated = _uiState.value.businessHours.toMutableList()
        if (dayIndex !in updated.indices) return
        updated[dayIndex] = updated[dayIndex].copy(isOpen = isOpen)
        _uiState.value = _uiState.value.copy(businessHours = updated, saveSuccess = false)
    }

    fun updateDayOpensAt(dayIndex: Int, time: String) {
        val updated = _uiState.value.businessHours.toMutableList()
        if (dayIndex !in updated.indices) return
        updated[dayIndex] = updated[dayIndex].copy(opensAt = time)
        _uiState.value = _uiState.value.copy(businessHours = updated, saveSuccess = false)
    }

    fun updateDayClosesAt(dayIndex: Int, time: String) {
        val updated = _uiState.value.businessHours.toMutableList()
        if (dayIndex !in updated.indices) return
        updated[dayIndex] = updated[dayIndex].copy(closesAt = time)
        _uiState.value = _uiState.value.copy(businessHours = updated, saveSuccess = false)
    }

    // --- Save ---

    fun save() {
        val state = _uiState.value
        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val laborCents = BillingEngine.parseDollarsToCents(state.laborRateDisplay) ?: 12500L
            val feeCents = BillingEngine.parseDollarsToCents(state.serviceFeeDisplay) ?: 6000L
            val markupBp = BillingEngine.parsePercentToBasisPoints(state.partsMarkupDisplay) ?: 14000
            val taxBp = BillingEngine.parsePercentToBasisPoints(state.taxRateDisplay) ?: 600

            // Serialize business hours to JSON
            val schedule = BusinessHoursSchedule(
                days = state.businessHours.map { day ->
                    DaySchedule(
                        isOpen = day.isOpen,
                        openHHMM = day.opensAt.ifBlank { "08:00" },
                        closeHHMM = day.closesAt.ifBlank { "17:00" }
                    )
                }
            )
            val hoursJson = Json.encodeToString(BusinessHoursSchedule.serializer(), schedule)

            val profile = ShopProfileEntity(
                id = currentProfileId,
                businessName = state.businessName.ifBlank { null },
                businessAddress = state.businessAddress.ifBlank { null },
                ownerName = state.ownerName.ifBlank { null },
                ownerPhone = state.ownerPhone.ifBlank { null },
                laborRateCents = laborCents,
                serviceFeeCents = feeCents,
                partsMarkupBasisPoints = markupBp,
                taxRateBasisPoints = taxBp,
                taxId = state.taxId.ifBlank { null },
                licenseNumber = state.licenseNumber.ifBlank { null },
                autoReplyEnabled = state.autoReplyEnabled,
                autoReplyMessage = state.autoReplyMessage.ifBlank { null },
                businessHoursJson = hoursJson
            )

            operationsRepository.saveShopProfile(profile)
            _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
        }
    }

    // --- Conversion helpers ---

    private fun formatCentsToInput(cents: Long): String {
        return String.format("%.2f", cents / 100.0)
    }

    private fun formatBasisPointsToInput(basisPoints: Int): String {
        return String.format("%.2f", basisPoints / 100.0)
    }
}

package com.phillips.phill.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.ShopProfileEntity
import com.phillips.phill.data.repository.OperationsRepository
import com.phillips.phill.domain.billing.BillingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
                // First launch: seed with defaults per anchoring document §7
                profile = ShopProfileEntity()
                operationsRepository.saveShopProfile(profile)
            }
            currentProfileId = profile.id
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
                isLoaded = true
            )
        }
    }

    // --- Field update methods ---

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

    fun save() {
        val state = _uiState.value
        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val laborCents = BillingEngine.parseDollarsToCents(state.laborRateDisplay) ?: 12500L
            val feeCents = BillingEngine.parseDollarsToCents(state.serviceFeeDisplay) ?: 6000L
            val markupBp = BillingEngine.parsePercentToBasisPoints(state.partsMarkupDisplay) ?: 14000
            val taxBp = BillingEngine.parsePercentToBasisPoints(state.taxRateDisplay) ?: 600

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
                licenseNumber = state.licenseNumber.ifBlank { null }
            )

            operationsRepository.saveShopProfile(profile)
            _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
        }
    }

    // --- Conversion helpers ---

    /** Convert cents Long to display string: 12500L -> "125.00" */
    private fun formatCentsToInput(cents: Long): String {
        return String.format("%.2f", cents / 100.0)
    }

    /** Convert basis points Int to display string: 14000 -> "140.00", 600 -> "6.00" */
    private fun formatBasisPointsToInput(basisPoints: Int): String {
        return String.format("%.2f", basisPoints / 100.0)
    }
}

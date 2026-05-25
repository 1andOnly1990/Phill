package com.phillips.phill.ui.customers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.VehicleEntity
import com.phillips.phill.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehicleFormUiState(
    val year: String = "",
    val make: String = "",
    val model: String = "",
    val engine: String = "",
    val vin: String = "",
    val color: String = "",
    val notes: String = "",
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val makeModelError: String? = null
)

@HiltViewModel
class VehicleFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val customerId: String = savedStateHandle.get<String>("customerId") ?: ""
    private val vehicleId: String? = savedStateHandle.get<String>("vehicleId")

    private val _uiState = MutableStateFlow(VehicleFormUiState())
    val uiState: StateFlow<VehicleFormUiState> = _uiState.asStateFlow()

    private var existingVehicle: VehicleEntity? = null

    init {
        if (vehicleId != null) {
            loadExisting(vehicleId)
        }
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            val vehicle = customerRepository.getVehicleById(id)
            if (vehicle != null) {
                existingVehicle = vehicle
                _uiState.value = VehicleFormUiState(
                    year = vehicle.year?.toString() ?: "",
                    make = vehicle.make,
                    model = vehicle.model,
                    engine = vehicle.engine ?: "",
                    vin = vehicle.vin ?: "",
                    color = vehicle.color ?: "",
                    notes = vehicle.notes ?: "",
                    isEditMode = true
                )
            }
        }
    }

    fun updateYear(value: String) { _uiState.value = _uiState.value.copy(year = value) }
    fun updateMake(value: String) { _uiState.value = _uiState.value.copy(make = value, makeModelError = null) }
    fun updateModel(value: String) { _uiState.value = _uiState.value.copy(model = value, makeModelError = null) }
    fun updateEngine(value: String) { _uiState.value = _uiState.value.copy(engine = value) }
    fun updateVin(value: String) { _uiState.value = _uiState.value.copy(vin = value) }
    fun updateColor(value: String) { _uiState.value = _uiState.value.copy(color = value) }
    fun updateNotes(value: String) { _uiState.value = _uiState.value.copy(notes = value) }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value

        if (state.make.isBlank() || state.model.isBlank()) {
            _uiState.value = state.copy(makeModelError = "Make and model are required")
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val vehicle = if (existingVehicle != null) {
                existingVehicle!!.copy(
                    year = state.year.toIntOrNull(),
                    make = state.make.trim(),
                    model = state.model.trim(),
                    engine = state.engine.trim().ifBlank { null },
                    vin = state.vin.trim().ifBlank { null },
                    color = state.color.trim().ifBlank { null },
                    notes = state.notes.trim().ifBlank { null }
                )
            } else {
                VehicleEntity(
                    customerId = customerId,
                    year = state.year.toIntOrNull(),
                    make = state.make.trim(),
                    model = state.model.trim(),
                    engine = state.engine.trim().ifBlank { null },
                    vin = state.vin.trim().ifBlank { null },
                    color = state.color.trim().ifBlank { null },
                    notes = state.notes.trim().ifBlank { null }
                )
            }

            customerRepository.saveVehicle(vehicle)
            _uiState.value = _uiState.value.copy(isSaving = false)
            onSuccess()
        }
    }
}

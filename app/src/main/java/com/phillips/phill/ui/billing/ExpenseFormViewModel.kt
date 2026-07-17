package com.phillips.phill.ui.billing


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.ExpenseEntity
import com.phillips.phill.data.repository.OperationsRepository
import com.phillips.phill.domain.billing.BillingEngine
import com.phillips.phill.domain.enums.ExpenseCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpenseFormUiState(
    val amountDisplay: String = "",
    val description: String = "",
    val vendor: String = "",
    val category: ExpenseCategory = ExpenseCategory.PARTS,
    val notes: String = "",
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val validationError: String? = null,
    val receiptUri: String? = null
)

@HiltViewModel
class ExpenseFormViewModel @Inject constructor(
    private val operationsRepository: OperationsRepository
) : ViewModel() {

    private var jobId: String? = null
    private var expenseId: String? = null
    private var initialized = false

    private val _uiState = MutableStateFlow(ExpenseFormUiState())
    val uiState: StateFlow<ExpenseFormUiState> = _uiState.asStateFlow()

    private var existingExpense: ExpenseEntity? = null

    fun initialize(jobId: String?, expenseId: String?) {
        if (initialized) return
        initialized = true
        this.jobId = jobId
        this.expenseId = expenseId
        if (expenseId != null) {
            loadExisting(expenseId)
        }
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            val expense = operationsRepository.getExpenseById(id) ?: return@launch
            existingExpense = expense
            _uiState.value = ExpenseFormUiState(
                amountDisplay = BillingEngine.formatCents(expense.amountCents).removePrefix("$"),
                description = expense.description,
                vendor = expense.vendor ?: "",
                category = expense.category,
                notes = expense.notes ?: "",
                isEditMode = true,
                receiptUri = expense.receiptUri
            )
        }
    }

    fun updateAmount(value: String) { _uiState.value = _uiState.value.copy(amountDisplay = value, validationError = null) }
    fun updateDescription(value: String) { _uiState.value = _uiState.value.copy(description = value, validationError = null) }
    fun updateVendor(value: String) { _uiState.value = _uiState.value.copy(vendor = value) }
    fun updateCategory(category: ExpenseCategory) { _uiState.value = _uiState.value.copy(category = category) }
    fun updateNotes(value: String) { _uiState.value = _uiState.value.copy(notes = value) }

    fun attachReceipt(uri: String) {
        _uiState.value = _uiState.value.copy(receiptUri = uri)
    }

    fun removeReceipt() {
        _uiState.value = _uiState.value.copy(receiptUri = null)
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return // Guard check to prevent double clicks

        val amountCents = BillingEngine.parseDollarsToCents(state.amountDisplay)
        if (amountCents == null || amountCents <= 0) {
            _uiState.value = state.copy(validationError = "Enter a valid amount")
            return
        }
        if (state.description.isBlank()) {
            _uiState.value = state.copy(validationError = "Description is required")
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val expense = if (existingExpense != null) {
                existingExpense!!.copy(
                    amountCents = amountCents,
                    description = state.description.trim(),
                    vendor = state.vendor.trim().ifBlank { null },
                    category = state.category,
                    notes = state.notes.trim().ifBlank { null },
                    receiptUri = state.receiptUri
                )
            } else {
                ExpenseEntity(
                    jobId = jobId,
                    amountCents = amountCents,
                    description = state.description.trim(),
                    vendor = state.vendor.trim().ifBlank { null },
                    category = state.category,
                    dateEpoch = System.currentTimeMillis(),
                    notes = state.notes.trim().ifBlank { null },
                    receiptUri = state.receiptUri
                )
            }

            operationsRepository.saveExpense(expense)
            _uiState.value = _uiState.value.copy(isSaving = false)
            onSuccess()
        }
    }
}

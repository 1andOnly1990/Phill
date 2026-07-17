package com.phillips.phill.ui.billing


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.InvoiceEntity
import com.phillips.phill.data.entity.LineItemEntity
import com.phillips.phill.data.entity.PaymentEntity
import com.phillips.phill.data.repository.BillingRepository
import com.phillips.phill.data.repository.CustomerRepository
import com.phillips.phill.data.repository.JobRepository
import com.phillips.phill.data.repository.OperationsRepository
import com.phillips.phill.domain.billing.BillingEngine
import com.phillips.phill.domain.billing.InvoicePdfGenerator
import com.phillips.phill.domain.enums.InvoiceStatus
import com.phillips.phill.domain.enums.PaymentMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class InvoiceDetailUiState(
    val invoice: InvoiceEntity? = null,
    val payments: List<PaymentEntity> = emptyList(),
    val lineItems: List<LineItemEntity> = emptyList(),
    val totalPaidCents: Long = 0L,
    val remainingCents: Long = 0L,
    val isLoading: Boolean = true,
    val pdfFile: File? = null,
    val pdfReady: Boolean = false,
    // Payment form
    val showPaymentDialog: Boolean = false,
    val paymentAmountDisplay: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paymentReference: String = "",
    val paymentNotes: String = ""
)

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val customerRepository: CustomerRepository,
    private val jobRepository: JobRepository,
    private val operationsRepository: OperationsRepository
) : ViewModel() {

    private var invoiceId: String = ""
    private var initialized = false

    private val _uiState = MutableStateFlow(InvoiceDetailUiState())
    val uiState: StateFlow<InvoiceDetailUiState> = _uiState.asStateFlow()

    fun initialize(invoiceId: String) {
        if (initialized) return
        initialized = true
        this.invoiceId = invoiceId
        loadInvoice()
    }

    private fun loadInvoice() {
        viewModelScope.launch {
            val invoice = billingRepository.getInvoiceById(invoiceId)
            if (invoice == null) {
                _uiState.value = InvoiceDetailUiState(isLoading = false)
                return@launch
            }

            billingRepository.observePaymentsByInvoice(invoiceId)
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                .collect { payments ->
                    val totalPaid = payments.sumOf { it.amountCents }
                    val remaining = invoice.totalCents - totalPaid
                    val lineItems = billingRepository.observeLineItems(invoiceId).first()
                    _uiState.value = InvoiceDetailUiState(
                        invoice = invoice,
                        payments = payments,
                        lineItems = lineItems,
                        totalPaidCents = totalPaid,
                        remainingCents = remaining,
                        isLoading = false
                    )
                }
        }
    }

    fun showPaymentDialog() {
        val remaining = _uiState.value.remainingCents
        _uiState.value = _uiState.value.copy(
            showPaymentDialog = true,
            paymentAmountDisplay = BillingEngine.formatCents(remaining).removePrefix("$"),
            paymentMethod = PaymentMethod.CASH,
            paymentReference = "",
            paymentNotes = ""
        )
    }

    fun dismissPaymentDialog() {
        _uiState.value = _uiState.value.copy(showPaymentDialog = false)
    }

    fun updatePaymentAmount(value: String) { _uiState.value = _uiState.value.copy(paymentAmountDisplay = value) }
    fun updatePaymentMethod(method: PaymentMethod) { _uiState.value = _uiState.value.copy(paymentMethod = method) }
    fun updatePaymentReference(value: String) { _uiState.value = _uiState.value.copy(paymentReference = value) }
    fun updatePaymentNotes(value: String) { _uiState.value = _uiState.value.copy(paymentNotes = value) }

    fun savePayment() {
        val amount = BillingEngine.parseDollarsToCents(_uiState.value.paymentAmountDisplay) ?: return

        viewModelScope.launch {
            val payment = PaymentEntity(
                invoiceId = invoiceId,
                amountCents = amount,
                method = _uiState.value.paymentMethod,
                referenceNumber = _uiState.value.paymentReference.trim().ifBlank { null },
                paidAtEpoch = System.currentTimeMillis(),
                notes = _uiState.value.paymentNotes.trim().ifBlank { null }
            )
            billingRepository.savePayment(payment)

            // Check if fully paid
            val totalPaid = billingRepository.totalPaidForInvoice(invoiceId)
            val invoice = _uiState.value.invoice
            if (invoice != null && totalPaid >= invoice.totalCents) {
                billingRepository.saveInvoice(invoice.copy(status = InvoiceStatus.PAID))
            }

            _uiState.value = _uiState.value.copy(showPaymentDialog = false)
        }
    }

    fun markFullyPaid() {
        showPaymentDialog()
    }

    fun generatePdf(context: android.content.Context) {
        viewModelScope.launch {
            val invoice = _uiState.value.invoice ?: return@launch
            val lineItems = _uiState.value.lineItems
            val job = jobRepository.getJobById(invoice.jobId)
            val customer = customerRepository.getCustomerById(invoice.customerId)
            val vehicle = job?.let { customerRepository.getVehicleById(it.vehicleId) }
            val shopProfile = operationsRepository.getShopProfile()

            val file = InvoicePdfGenerator.generate(
                context = context,
                invoice = invoice,
                lineItems = lineItems,
                customer = customer,
                vehicle = vehicle,
                shopProfile = shopProfile
            )
            _uiState.value = _uiState.value.copy(pdfFile = file, pdfReady = true)
        }
    }

    fun clearPdfState() {
        _uiState.value = _uiState.value.copy(pdfReady = false)
    }
}

package com.phillips.phill.ui.billing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.InvoiceEntity
import com.phillips.phill.data.entity.LineItemEntity
import com.phillips.phill.data.entity.ShopProfileEntity
import com.phillips.phill.data.repository.BillingRepository
import com.phillips.phill.data.repository.CustomerRepository
import com.phillips.phill.data.repository.JobRepository
import com.phillips.phill.data.repository.OperationsRepository
import com.phillips.phill.domain.billing.BillingEngine
import com.phillips.phill.domain.billing.InvoiceTotals
import com.phillips.phill.domain.billing.LineItemTotal
import com.phillips.phill.domain.enums.InvoiceStatus
import com.phillips.phill.domain.enums.LineItemType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LineItemUiModel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: LineItemType = LineItemType.LABOR,
    val description: String = "",
    val quantityDisplay: String = "",
    val unitPriceDisplay: String = "",
    val isTaxable: Boolean = false,
    val sortOrder: Int = 0
)

data class InvoiceBuilderUiState(
    val jobId: String = "",
    val invoiceId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val vehicleDesc: String = "",
    val status: InvoiceStatus = InvoiceStatus.ESTIMATE,
    val lineItems: List<LineItemUiModel> = emptyList(),
    val serviceFeeCentsDisplay: String = "60.00",
    val laborRateCents: Long = 12500L,
    val partsMarkupBasisPoints: Int = 14000,
    val taxRateBasisPoints: Int = 600,
    val totals: InvoiceTotals = InvoiceTotals(0, 0, 0, 0, 0, 0, 0),
    val legalClause: String = "Estimate valid for 30 days. Customer authorizes the described repairs. All parts and labor guaranteed for 12 months or 12,000 miles, whichever comes first.",
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false,
    val isEditMode: Boolean = false
)

@HiltViewModel
class InvoiceBuilderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val billingRepository: BillingRepository,
    private val jobRepository: JobRepository,
    private val customerRepository: CustomerRepository,
    private val operationsRepository: OperationsRepository
) : ViewModel() {

    private val jobId: String = savedStateHandle.get<String>("jobId") ?: ""
    private val invoiceId: String? = savedStateHandle.get<String>("invoiceId")

    private val _uiState = MutableStateFlow(InvoiceBuilderUiState())
    val uiState: StateFlow<InvoiceBuilderUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load shop settings for defaults
            val profile = operationsRepository.getShopProfile() ?: ShopProfileEntity()

            if (invoiceId != null && invoiceId != jobId) {
                // Editing existing invoice
                val invoice = billingRepository.getInvoiceById(invoiceId)
                if (invoice != null) {
                    loadExistingInvoice(invoice, profile)
                    return@launch
                }
            }

            // New estimate for this job
            val job = jobRepository.getJobById(jobId)
            val customer = job?.let { customerRepository.getCustomerById(it.customerId) }
            val vehicle = job?.let { customerRepository.getVehicleById(it.vehicleId) }

            val vehicleDesc = vehicle?.let { v ->
                buildString {
                    v.year?.let { append("$it ") }
                    append("${v.make} ${v.model}")
                }
            } ?: ""

            _uiState.value = InvoiceBuilderUiState(
                jobId = jobId,
                invoiceId = java.util.UUID.randomUUID().toString(),
                customerId = job?.customerId ?: "",
                customerName = customer?.let { "${it.firstName} ${it.lastName}" } ?: "",
                vehicleDesc = vehicleDesc,
                serviceFeeCentsDisplay = BillingEngine.formatCents(profile.serviceFeeCents).removePrefix("$"),
                laborRateCents = profile.laborRateCents,
                partsMarkupBasisPoints = profile.partsMarkupBasisPoints,
                taxRateBasisPoints = profile.taxRateBasisPoints,
                isLoaded = true
            )
        }
    }

    private suspend fun loadExistingInvoice(invoice: InvoiceEntity, profile: ShopProfileEntity) {
        val customer = customerRepository.getCustomerById(invoice.customerId)
        val job = jobRepository.getJobById(invoice.jobId)
        val vehicle = job?.let { customerRepository.getVehicleById(it.vehicleId) }

        val vehicleDesc = vehicle?.let { v ->
            buildString {
                v.year?.let { append("$it ") }
                append("${v.make} ${v.model}")
            }
        } ?: ""

        // Load line items
        val lineItemEntities = mutableListOf<LineItemEntity>()
        billingRepository.observeLineItems(invoice.id).collect { items ->
            lineItemEntities.clear()
            lineItemEntities.addAll(items)
        }

        val uiLineItems = lineItemEntities.map { entity ->
            LineItemUiModel(
                id = entity.id,
                type = entity.type,
                description = entity.description,
                quantityDisplay = BillingEngine.formatThousandths(entity.quantityThousandths),
                unitPriceDisplay = BillingEngine.formatCents(entity.unitPriceCents).removePrefix("$"),
                isTaxable = entity.isTaxable,
                sortOrder = entity.sortOrder
            )
        }

        _uiState.value = InvoiceBuilderUiState(
            jobId = invoice.jobId,
            invoiceId = invoice.id,
            customerId = invoice.customerId,
            customerName = customer?.let { "${it.firstName} ${it.lastName}" } ?: "",
            vehicleDesc = vehicleDesc,
            status = invoice.status,
            lineItems = uiLineItems,
            serviceFeeCentsDisplay = BillingEngine.formatCents(invoice.serviceFeeCents).removePrefix("$"),
            laborRateCents = profile.laborRateCents,
            partsMarkupBasisPoints = profile.partsMarkupBasisPoints,
            taxRateBasisPoints = profile.taxRateBasisPoints,
            legalClause = invoice.termsText ?: InvoiceBuilderUiState().legalClause,
            isLoaded = true,
            isEditMode = true
        )
        recalculate()
    }

    // --- Line item management ---

    fun addLineItem(type: LineItemType) {
        val items = _uiState.value.lineItems.toMutableList()
        val defaultPrice = when (type) {
            LineItemType.LABOR -> BillingEngine.formatCents(_uiState.value.laborRateCents).removePrefix("$")
            LineItemType.PARTS -> ""
            LineItemType.MISC -> ""
        }
        items.add(
            LineItemUiModel(
                type = type,
                description = "",
                quantityDisplay = if (type == LineItemType.LABOR) "1.0" else "1",
                unitPriceDisplay = defaultPrice,
                isTaxable = type == LineItemType.PARTS, // Tax on parts only (SC §117-306)
                sortOrder = items.size
            )
        )
        _uiState.value = _uiState.value.copy(lineItems = items)
        recalculate()
    }

    fun updateLineItem(index: Int, item: LineItemUiModel) {
        val items = _uiState.value.lineItems.toMutableList()
        if (index in items.indices) {
            items[index] = item
            _uiState.value = _uiState.value.copy(lineItems = items)
            recalculate()
        }
    }

    fun removeLineItem(index: Int) {
        val items = _uiState.value.lineItems.toMutableList()
        if (index in items.indices) {
            items.removeAt(index)
            _uiState.value = _uiState.value.copy(lineItems = items)
            recalculate()
        }
    }

    fun updateServiceFee(value: String) {
        _uiState.value = _uiState.value.copy(serviceFeeCentsDisplay = value)
        recalculate()
    }

    fun updateLegalClause(value: String) {
        _uiState.value = _uiState.value.copy(legalClause = value)
    }

    private fun recalculate() {
        val state = _uiState.value
        val serviceFeeCents = BillingEngine.parseDollarsToCents(state.serviceFeeCentsDisplay) ?: 0L

        val lineItemTotals = state.lineItems.mapNotNull { item ->
            val qty = BillingEngine.parseHoursToThousandths(item.quantityDisplay) ?: return@mapNotNull null
            val price = BillingEngine.parseDollarsToCents(item.unitPriceDisplay) ?: return@mapNotNull null

            // Apply parts markup if PARTS type
            val effectivePrice = if (item.type == LineItemType.PARTS) {
                BillingEngine.applyPartsMarkup(price, state.partsMarkupBasisPoints)
            } else {
                price
            }

            LineItemTotal(
                totalCents = BillingEngine.calculateLineItemTotal(qty, effectivePrice),
                isTaxable = item.isTaxable,
                isLabor = item.type == LineItemType.LABOR
            )
        }

        val totals = BillingEngine.calculateInvoiceTotal(lineItemTotals, serviceFeeCents, state.taxRateBasisPoints)
        _uiState.value = state.copy(totals = totals)
    }

    // --- Save ---

    fun saveAsEstimate(onSuccess: () -> Unit) {
        save(InvoiceStatus.ESTIMATE, onSuccess)
    }

    fun finalizeAsInvoice(onSuccess: () -> Unit) {
        // Per user rule: estimates must precede invoices. Only finalize if currently ESTIMATE.
        val currentStatus = _uiState.value.status
        if (currentStatus != InvoiceStatus.ESTIMATE) return
        save(InvoiceStatus.INVOICE, onSuccess)
    }

    private fun save(status: InvoiceStatus, onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val serviceFeeCents = BillingEngine.parseDollarsToCents(state.serviceFeeCentsDisplay) ?: 0L

            val invoice = InvoiceEntity(
                id = state.invoiceId,
                jobId = state.jobId,
                customerId = state.customerId,
                status = status,
                subtotalCents = state.totals.laborSubtotalCents + state.totals.partsSubtotalCents + state.totals.miscSubtotalCents,
                taxCents = state.totals.taxCents,
                totalCents = state.totals.grandTotalCents,
                serviceFeeCents = serviceFeeCents,
                finalizedAtEpoch = if (status == InvoiceStatus.INVOICE) System.currentTimeMillis() else null,
                termsText = state.legalClause
            )

            billingRepository.saveInvoice(invoice)

            // Delete old line items and re-insert
            billingRepository.deleteLineItemsByInvoice(state.invoiceId)

            val lineItemEntities = state.lineItems.mapIndexedNotNull { index, item ->
                val qty = BillingEngine.parseHoursToThousandths(item.quantityDisplay) ?: return@mapIndexedNotNull null
                val rawPrice = BillingEngine.parseDollarsToCents(item.unitPriceDisplay) ?: return@mapIndexedNotNull null

                val effectivePrice = if (item.type == LineItemType.PARTS) {
                    BillingEngine.applyPartsMarkup(rawPrice, state.partsMarkupBasisPoints)
                } else {
                    rawPrice
                }

                LineItemEntity(
                    id = item.id,
                    invoiceId = state.invoiceId,
                    type = item.type,
                    description = item.description,
                    quantityThousandths = qty,
                    unitPriceCents = effectivePrice,
                    totalCents = BillingEngine.calculateLineItemTotal(qty, effectivePrice),
                    isTaxable = item.isTaxable,
                    sortOrder = index
                )
            }

            billingRepository.saveAllLineItems(lineItemEntities)
            _uiState.value = _uiState.value.copy(isSaving = false, status = status)
            onSuccess()
        }
    }
}

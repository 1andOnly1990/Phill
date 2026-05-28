package com.phillips.phill.data.repository

import com.phillips.phill.data.dao.InvoiceDao
import com.phillips.phill.data.dao.LineItemDao
import com.phillips.phill.data.dao.PaymentDao
import com.phillips.phill.data.entity.InvoiceEntity
import com.phillips.phill.data.entity.LineItemEntity
import com.phillips.phill.data.entity.PaymentEntity
import com.phillips.phill.domain.enums.InvoiceStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val lineItemDao: LineItemDao,
    private val paymentDao: PaymentDao
) {
    // Invoices
    fun observeAllInvoices(): Flow<List<InvoiceEntity>> = invoiceDao.observeAll()

    fun observeInvoicesByJob(jobId: String): Flow<List<InvoiceEntity>> =
        invoiceDao.observeByJob(jobId)

    fun observeInvoicesByCustomer(customerId: String): Flow<List<InvoiceEntity>> =
        invoiceDao.observeByCustomer(customerId)

    fun observeInvoicesByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>> =
        invoiceDao.observeByStatus(status)

    suspend fun getInvoiceById(id: String): InvoiceEntity? = invoiceDao.getById(id)

    suspend fun saveInvoice(invoice: InvoiceEntity) {
        val existing = invoiceDao.getById(invoice.id)
        if (existing != null) invoiceDao.update(invoice) else invoiceDao.insert(invoice)
    }

    suspend fun deleteInvoice(invoice: InvoiceEntity) = invoiceDao.delete(invoice)

    fun observeInvoicesByVehicle(vehicleId: String): Flow<List<InvoiceEntity>> =
        invoiceDao.observeByVehicle(vehicleId)

    suspend fun generateInvoiceNumber(isEstimate: Boolean): String {
        val prefix = if (isEstimate) "EST-" else "INV-"
        val maxNum = invoiceDao.getMaxNumberForPrefix(prefix) ?: 0
        return "$prefix${String.format("%03d", maxNum + 1)}"
    }

    // Line items
    fun observeLineItems(invoiceId: String): Flow<List<LineItemEntity>> =
        lineItemDao.observeByInvoice(invoiceId)

    suspend fun saveLineItem(lineItem: LineItemEntity) {
        val existing = lineItemDao.getById(lineItem.id)
        if (existing != null) lineItemDao.update(lineItem) else lineItemDao.insert(lineItem)
    }

    suspend fun saveAllLineItems(lineItems: List<LineItemEntity>) =
        lineItemDao.insertAll(lineItems)

    suspend fun deleteLineItem(lineItem: LineItemEntity) = lineItemDao.delete(lineItem)

    suspend fun deleteLineItemsByInvoice(invoiceId: String) =
        lineItemDao.deleteByInvoice(invoiceId)

    // Payments
    fun observePaymentsByInvoice(invoiceId: String): Flow<List<PaymentEntity>> =
        paymentDao.observeByInvoice(invoiceId)

    fun observeAllPayments(): Flow<List<PaymentEntity>> = paymentDao.observeAll()

    suspend fun getPaymentById(id: String): PaymentEntity? = paymentDao.getById(id)

    suspend fun totalPaidForInvoice(invoiceId: String): Long =
        paymentDao.totalPaidForInvoice(invoiceId) ?: 0L

    suspend fun savePayment(payment: PaymentEntity) {
        val existing = paymentDao.getById(payment.id)
        if (existing != null) paymentDao.update(payment) else paymentDao.insert(payment)
    }

    suspend fun deletePayment(payment: PaymentEntity) = paymentDao.delete(payment)
}

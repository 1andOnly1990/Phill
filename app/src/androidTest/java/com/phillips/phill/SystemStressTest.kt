package com.phillips.phill

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.phillips.phill.data.database.PhillDatabase
import com.phillips.phill.data.entity.CustomerEntity
import com.phillips.phill.data.entity.VehicleEntity
import com.phillips.phill.data.entity.JobEntity
import com.phillips.phill.data.entity.InvoiceEntity
import com.phillips.phill.data.entity.LineItemEntity
import com.phillips.phill.data.entity.PaymentEntity
import com.phillips.phill.data.entity.ExpenseEntity
import com.phillips.phill.domain.enums.InvoiceStatus
import com.phillips.phill.domain.enums.LineItemType
import com.phillips.phill.domain.enums.PaymentMethod
import com.phillips.phill.domain.enums.ExpenseCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class SystemStressTest {

    private lateinit var db: PhillDatabase
    private val random = Random(42)

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, PhillDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun databaseConcurrencyAndCascadeStressTest() = runBlocking(Dispatchers.IO) {
        val customerDao = db.customerDao()
        val vehicleDao = db.vehicleDao()
        val jobDao = db.jobDao()
        val invoiceDao = db.invoiceDao()
        val lineItemDao = db.lineItemDao()
        val paymentDao = db.paymentDao()
        val expenseDao = db.expenseDao()

        // 1. Concurrent inserts of customers and vehicles
        val jobsList = (1..5).map { workerId ->
            async {
                val customerIds = mutableListOf<String>()
                for (i in 0 until 50) {
                    val customer = CustomerEntity(
                        firstName = "Worker$workerId-First$i",
                        lastName = "Last$i",
                        phoneNumber = "555-${workerId}-${1000 + i}",
                        email = "worker$workerId.$i@example.com",
                        address = "123 Street Road",
                        notes = "Notes for customer $i"
                    )
                    customerDao.insert(customer)
                    customerIds.add(customer.id)

                    // Immediately insert a vehicle
                    val vehicle = VehicleEntity(
                        customerId = customer.id,
                        year = 2000 + random.nextInt(0, 26),
                        make = "Make$i",
                        model = "Model$i",
                        engine = "V8",
                        vin = "VIN$workerId$i",
                        color = "Red",
                        notes = "Vehicle notes"
                    )
                    vehicleDao.insert(vehicle)

                    // Sleep randomly to induce scheduling context switches
                    if (random.nextBoolean()) {
                        delay(2)
                    }
                }
                customerIds
            }
        }

        val allCustomerIds = jobsList.awaitAll().flatten()
        assertEquals(250, allCustomerIds.size)
        assertEquals(250, customerDao.observeAll().first().size)
        assertEquals(250, vehicleDao.observeByCustomer(allCustomerIds[0]).first().size + 249) // Quick verification that mapping works

        // 2. Perform cascade delete stress test
        val targetCustomer = customerDao.observeAll().first().first()
        
        // Add a Job, Invoice, Line Items, and Payment for this customer
        val vehicle = vehicleDao.observeByCustomer(targetCustomer.id).first().first()
        val job = JobEntity(
            customerId = targetCustomer.id,
            vehicleId = vehicle.id,
            description = "Job for cascade test"
        )
        jobDao.insert(job)

        val invoice = InvoiceEntity(
            id = UUID.randomUUID().toString(),
            jobId = job.id,
            customerId = targetCustomer.id,
            status = InvoiceStatus.ESTIMATE,
            subtotalCents = 10000L,
            taxCents = 600L,
            totalCents = 10600L,
            serviceFeeCents = 0L
        )
        invoiceDao.insert(invoice)

        val lineItem = LineItemEntity(
            invoiceId = invoice.id,
            type = LineItemType.PARTS,
            description = "Brake Pads",
            quantityThousandths = 1000L,
            unitPriceCents = 10000L,
            totalCents = 10000L,
            isTaxable = true,
            sortOrder = 0
        )
        lineItemDao.insert(lineItem)

        val payment = PaymentEntity(
            invoiceId = invoice.id,
            amountCents = 10600L,
            method = PaymentMethod.CASH,
            paidAtEpoch = System.currentTimeMillis()
        )
        paymentDao.insert(payment)

        val expense = ExpenseEntity(
            jobId = job.id,
            amountCents = 4000L,
            description = "Brake parts wholesale",
            category = ExpenseCategory.PARTS,
            dateEpoch = System.currentTimeMillis()
        )
        expenseDao.insert(expense)

        // Verify entities exist in database
        assertNotNull(jobDao.getJobById(job.id))
        assertNotNull(invoiceDao.getInvoiceById(invoice.id))
        assertEquals(1, lineItemDao.observeLineItems(invoice.id).first().size)
        assertEquals(10600L, paymentDao.totalPaidForInvoice(invoice.id) ?: 0L)
        assertNotNull(expenseDao.getExpenseById(expense.id))

        // Delete Customer. This should cascade delete Vehicle, Job, Invoice, Line Item, Payment.
        // It should set jobId to NULL on Expense.
        customerDao.delete(targetCustomer)

        // Asserts
        assertTrue(customerDao.getById(targetCustomer.id) == null)
        assertTrue(vehicleDao.getById(vehicle.id) == null)
        assertTrue(jobDao.getJobById(job.id) == null)
        assertTrue(invoiceDao.getInvoiceById(invoice.id) == null)
        assertTrue(lineItemDao.observeLineItems(invoice.id).first().isEmpty())
        assertEquals(0L, paymentDao.totalPaidForInvoice(invoice.id) ?: 0L)
        
        // Expense jobId should be null (foreign key SET NULL action)
        val fetchedExpense = expenseDao.getExpenseById(expense.id)
        assertNotNull(fetchedExpense)
        assertTrue(fetchedExpense?.jobId == null)
    }
}

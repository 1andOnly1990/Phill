package com.phillips.phill

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.phillips.phill.data.database.PhillDatabase
import com.phillips.phill.data.entity.CustomerEntity
import com.phillips.phill.data.repository.*
import com.phillips.phill.ui.schedule.AppointmentFormViewModel
import com.phillips.phill.ui.customers.CustomerFormViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewModelStressTest {

    private lateinit var db: PhillDatabase
    private lateinit var customerRepository: CustomerRepository
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var jobRepository: JobRepository
    private lateinit var billingRepository: BillingRepository
    private lateinit var operationsRepository: OperationsRepository

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, PhillDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        customerRepository = CustomerRepository(db.customerDao(), db.vehicleDao())
        scheduleRepository = ScheduleRepository(db.appointmentDao())
        jobRepository = JobRepository(db.jobDao(), db.clockEntryDao(), db.mileageEntryDao())
        billingRepository = BillingRepository(db.invoiceDao(), db.lineItemDao(), db.paymentDao())
        operationsRepository = OperationsRepository(db.shopProfileDao(), db.expenseDao(), db.mileageEntryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun stressTestCustomerFormViewModelSave() = runBlocking {
        // Construct ViewModel on Main thread
        val viewModel = withContext(Dispatchers.Main) {
            CustomerFormViewModel(customerRepository).apply {
                initialize(null, null)
                updateFirstName("John")
                updateLastName("Doe")
                updatePhoneNumber("864-555-0100")
            }
        }

        // Stress test: simulate rapid save attempts (double clicks)
        // Verify that the isSaving guard prevents multiple insertions
        val jobs = (1..20).map {
            async(Dispatchers.Default) {
                withContext(Dispatchers.Main) {
                    viewModel.save { _ -> }
                }
            }
        }
        jobs.awaitAll()

        // Retrieve saved customers
        val customers = customerRepository.observeAllCustomers().first()
        // There should be exactly 1 customer, not 20!
        assertEquals(1, customers.size)
        assertEquals("John", customers[0].firstName)
        assertEquals("Doe", customers[0].lastName)
    }

    @Test
    fun stressTestAppointmentFormViewModelAndJobs() = runBlocking {
        // Create a customer first
        val customer = CustomerEntity(
            firstName = "Alice",
            lastName = "Smith",
            phoneNumber = "864-555-0200"
        )
        customerRepository.saveCustomer(customer)

        val viewModel = withContext(Dispatchers.Main) {
            AppointmentFormViewModel(scheduleRepository, customerRepository, jobRepository).apply {
                initialize(null, null)
                selectCustomer(customer)
                updateAddress("123 Main St")
                updateScheduledStartEpoch(System.currentTimeMillis())
            }
        }

        // Save appointment (which auto-creates a Job) concurrently
        val jobs = (1..20).map {
            async(Dispatchers.Default) {
                withContext(Dispatchers.Main) {
                    viewModel.save {}
                }
            }
        }
        jobs.awaitAll()

        val appointments = scheduleRepository.observeByDateRange(0, Long.MAX_VALUE).first()
        // Should only save once
        assertEquals(1, appointments.size)

        val activeJobs = jobRepository.observeAllJobs().first()
        // Linked job should only be created once
        assertEquals(1, activeJobs.size)
        assertEquals(customer.id, activeJobs[0].customerId)
    }
}

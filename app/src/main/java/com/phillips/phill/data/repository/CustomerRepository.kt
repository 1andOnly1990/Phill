package com.phillips.phill.data.repository

import com.phillips.phill.data.dao.CustomerDao
import com.phillips.phill.data.dao.VehicleDao
import com.phillips.phill.data.entity.CustomerEntity
import com.phillips.phill.data.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(
    private val customerDao: CustomerDao,
    private val vehicleDao: VehicleDao
) {
    fun observeAllCustomers(): Flow<List<CustomerEntity>> = customerDao.observeAll()

    fun searchCustomers(query: String): Flow<List<CustomerEntity>> = customerDao.search(query)

    suspend fun getCustomerById(id: String): CustomerEntity? = customerDao.getById(id)

    suspend fun getCustomerByPhone(phone: String): CustomerEntity? = customerDao.getByPhone(phone)

    suspend fun saveCustomer(customer: CustomerEntity) {
        val existing = customerDao.getById(customer.id)
        if (existing != null) {
            customerDao.update(customer.copy(updatedAtEpoch = System.currentTimeMillis()))
        } else {
            customerDao.insert(customer)
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity) = customerDao.delete(customer)

    // Vehicle operations scoped to customer
    fun observeVehicles(customerId: String): Flow<List<VehicleEntity>> =
        vehicleDao.observeByCustomer(customerId)

    suspend fun getVehicleById(id: String): VehicleEntity? = vehicleDao.getById(id)

    suspend fun saveVehicle(vehicle: VehicleEntity) {
        val existing = vehicleDao.getById(vehicle.id)
        if (existing != null) vehicleDao.update(vehicle) else vehicleDao.insert(vehicle)
    }

    suspend fun deleteVehicle(vehicle: VehicleEntity) = vehicleDao.delete(vehicle)
}

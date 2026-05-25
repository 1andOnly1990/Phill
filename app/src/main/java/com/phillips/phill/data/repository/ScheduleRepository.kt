package com.phillips.phill.data.repository

import com.phillips.phill.data.dao.AppointmentDao
import com.phillips.phill.data.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val appointmentDao: AppointmentDao
) {
    fun observeByDateRange(startEpoch: Long, endEpoch: Long): Flow<List<AppointmentEntity>> =
        appointmentDao.observeByDateRange(startEpoch, endEpoch)

    fun observeByCustomer(customerId: String): Flow<List<AppointmentEntity>> =
        appointmentDao.observeByCustomer(customerId)

    suspend fun getById(id: String): AppointmentEntity? = appointmentDao.getById(id)

    suspend fun saveAppointment(appointment: AppointmentEntity) {
        val existing = appointmentDao.getById(appointment.id)
        if (existing != null) appointmentDao.update(appointment) else appointmentDao.insert(appointment)
    }

    suspend fun deleteAppointment(appointment: AppointmentEntity) =
        appointmentDao.delete(appointment)
}

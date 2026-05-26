package com.phillips.phill.data.repository

import com.phillips.phill.data.dao.ClockEntryDao
import com.phillips.phill.data.dao.JobDao
import com.phillips.phill.data.entity.ClockEntryEntity
import com.phillips.phill.data.entity.JobEntity
import com.phillips.phill.domain.enums.JobStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepository @Inject constructor(
    private val jobDao: JobDao,
    private val clockEntryDao: ClockEntryDao
) {
    fun observeAllJobs(): Flow<List<JobEntity>> = jobDao.observeAll()

    fun observeByStatus(status: JobStatus): Flow<List<JobEntity>> = jobDao.observeByStatus(status)

    fun observeByCustomer(customerId: String): Flow<List<JobEntity>> =
        jobDao.observeByCustomer(customerId)

    suspend fun getJobById(id: String): JobEntity? = jobDao.getById(id)

    suspend fun saveJob(job: JobEntity) {
        val existing = jobDao.getById(job.id)
        if (existing != null) jobDao.update(job) else jobDao.insert(job)
    }

    suspend fun updateJobStatus(jobId: String, status: JobStatus) {
        val job = jobDao.getById(jobId) ?: return
        val updated = if (status == JobStatus.COMPLETE) {
            job.copy(status = status, completedAtEpoch = System.currentTimeMillis())
        } else {
            job.copy(status = status)
        }
        jobDao.update(updated)
    }

    suspend fun deleteJob(job: JobEntity) = jobDao.delete(job)

    // Clock operations scoped to job
    fun observeAllClockEntries(): Flow<List<ClockEntryEntity>> = clockEntryDao.observeAll()

    fun observeClockEntries(jobId: String): Flow<List<ClockEntryEntity>> =
        clockEntryDao.observeByJob(jobId)

    suspend fun getActiveClockEntry(): ClockEntryEntity? = clockEntryDao.getActiveEntry()

    suspend fun clockIn(jobId: String): ClockEntryEntity {
        val entry = ClockEntryEntity(jobId = jobId, clockInEpoch = System.currentTimeMillis())
        clockEntryDao.insert(entry)
        return entry
    }

    suspend fun clockOut(entryId: String, notes: String? = null) {
        val entry = clockEntryDao.getById(entryId) ?: return
        clockEntryDao.update(
            entry.copy(clockOutEpoch = System.currentTimeMillis(), notes = notes)
        )
    }

    suspend fun saveClockEntry(entry: ClockEntryEntity) {
        val existing = clockEntryDao.getById(entry.id)
        if (existing != null) clockEntryDao.update(entry) else clockEntryDao.insert(entry)
    }

    suspend fun deleteClockEntry(entry: ClockEntryEntity) = clockEntryDao.delete(entry)
}

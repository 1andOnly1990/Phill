package com.phillips.phill.data.repository

import com.phillips.phill.data.dao.ExpenseDao
import com.phillips.phill.data.dao.MileageEntryDao
import com.phillips.phill.data.dao.ShopProfileDao
import com.phillips.phill.data.entity.ExpenseEntity
import com.phillips.phill.data.entity.MileageEntryEntity
import com.phillips.phill.data.entity.ShopProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OperationsRepository @Inject constructor(
    private val shopProfileDao: ShopProfileDao,
    private val expenseDao: ExpenseDao,
    private val mileageEntryDao: MileageEntryDao
) {
    // Shop profile
    fun observeShopProfile(): Flow<ShopProfileEntity?> = shopProfileDao.observe()

    suspend fun getShopProfile(): ShopProfileEntity? = shopProfileDao.get()

    suspend fun saveShopProfile(profile: ShopProfileEntity) {
        val existing = shopProfileDao.get()
        if (existing != null) shopProfileDao.update(profile) else shopProfileDao.insert(profile)
    }

    // Expenses
    fun observeAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.observeAll()

    fun observeExpensesByJob(jobId: String): Flow<List<ExpenseEntity>> =
        expenseDao.observeByJob(jobId)

    suspend fun getExpenseById(id: String): ExpenseEntity? = expenseDao.getById(id)

    suspend fun totalExpensesInRange(startEpoch: Long, endEpoch: Long): Long =
        expenseDao.totalInRange(startEpoch, endEpoch) ?: 0L

    suspend fun saveExpense(expense: ExpenseEntity) {
        val existing = expenseDao.getById(expense.id)
        if (existing != null) expenseDao.update(expense) else expenseDao.insert(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.delete(expense)

    // Mileage
    fun observeAllMileage(): Flow<List<MileageEntryEntity>> = mileageEntryDao.observeAll()

    fun observeMileageByJob(jobId: String): Flow<List<MileageEntryEntity>> =
        mileageEntryDao.observeByJob(jobId)

    suspend fun getMileageById(id: String): MileageEntryEntity? = mileageEntryDao.getById(id)

    suspend fun totalMilesInRange(startEpoch: Long, endEpoch: Long): Double =
        mileageEntryDao.totalMilesInRange(startEpoch, endEpoch) ?: 0.0

    suspend fun saveMileage(entry: MileageEntryEntity) {
        val existing = mileageEntryDao.getById(entry.id)
        if (existing != null) mileageEntryDao.update(entry) else mileageEntryDao.insert(entry)
    }

    suspend fun deleteMileage(entry: MileageEntryEntity) = mileageEntryDao.delete(entry)
}

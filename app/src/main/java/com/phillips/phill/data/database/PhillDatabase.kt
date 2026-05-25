package com.phillips.phill.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.phillips.phill.data.dao.AppointmentDao
import com.phillips.phill.data.dao.ClockEntryDao
import com.phillips.phill.data.dao.ConversationDao
import com.phillips.phill.data.dao.CustomerDao
import com.phillips.phill.data.dao.ExpenseDao
import com.phillips.phill.data.dao.InvoiceDao
import com.phillips.phill.data.dao.JobDao
import com.phillips.phill.data.dao.LineItemDao
import com.phillips.phill.data.dao.MessageDao
import com.phillips.phill.data.dao.MileageEntryDao
import com.phillips.phill.data.dao.PaymentDao
import com.phillips.phill.data.dao.ShopProfileDao
import com.phillips.phill.data.dao.VehicleDao
import com.phillips.phill.data.entity.AppointmentEntity
import com.phillips.phill.data.entity.ClockEntryEntity
import com.phillips.phill.data.entity.ConversationEntity
import com.phillips.phill.data.entity.CustomerEntity
import com.phillips.phill.data.entity.ExpenseEntity
import com.phillips.phill.data.entity.InvoiceEntity
import com.phillips.phill.data.entity.JobEntity
import com.phillips.phill.data.entity.LineItemEntity
import com.phillips.phill.data.entity.MessageEntity
import com.phillips.phill.data.entity.MileageEntryEntity
import com.phillips.phill.data.entity.PaymentEntity
import com.phillips.phill.data.entity.ShopProfileEntity
import com.phillips.phill.data.entity.VehicleEntity

@Database(
    entities = [
        CustomerEntity::class,
        VehicleEntity::class,
        AppointmentEntity::class,
        JobEntity::class,
        ClockEntryEntity::class,
        MileageEntryEntity::class,
        InvoiceEntity::class,
        LineItemEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        ShopProfileEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PhillDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun jobDao(): JobDao
    abstract fun clockEntryDao(): ClockEntryDao
    abstract fun mileageEntryDao(): MileageEntryDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun lineItemDao(): LineItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun shopProfileDao(): ShopProfileDao
}

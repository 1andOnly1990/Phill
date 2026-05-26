package com.phillips.phill.di

import android.content.Context
import androidx.room.Room
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
import com.phillips.phill.data.database.PhillDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PhillDatabase {
        return Room.databaseBuilder(
            context,
            PhillDatabase::class.java,
            "phill.db"
        ).addMigrations(PhillDatabase.MIGRATION_1_2).build()
    }

    @Provides fun provideCustomerDao(db: PhillDatabase): CustomerDao = db.customerDao()
    @Provides fun provideVehicleDao(db: PhillDatabase): VehicleDao = db.vehicleDao()
    @Provides fun provideAppointmentDao(db: PhillDatabase): AppointmentDao = db.appointmentDao()
    @Provides fun provideJobDao(db: PhillDatabase): JobDao = db.jobDao()
    @Provides fun provideClockEntryDao(db: PhillDatabase): ClockEntryDao = db.clockEntryDao()
    @Provides fun provideMileageEntryDao(db: PhillDatabase): MileageEntryDao = db.mileageEntryDao()
    @Provides fun provideInvoiceDao(db: PhillDatabase): InvoiceDao = db.invoiceDao()
    @Provides fun provideLineItemDao(db: PhillDatabase): LineItemDao = db.lineItemDao()
    @Provides fun providePaymentDao(db: PhillDatabase): PaymentDao = db.paymentDao()
    @Provides fun provideExpenseDao(db: PhillDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideConversationDao(db: PhillDatabase): ConversationDao = db.conversationDao()
    @Provides fun provideMessageDao(db: PhillDatabase): MessageDao = db.messageDao()
    @Provides fun provideShopProfileDao(db: PhillDatabase): ShopProfileDao = db.shopProfileDao()
}

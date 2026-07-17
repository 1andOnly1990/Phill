package com.phillips.phill.data.database

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.phillips.phill.data.dao.AttachmentDao
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
import com.phillips.phill.data.entity.AttachmentEntity

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
        ShopProfileEntity::class,
        AttachmentEntity::class
    ],
    version = 7,
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
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN terms_text TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shop_profile ADD COLUMN auto_reply_enabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE shop_profile ADD COLUMN auto_reply_message TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE shop_profile ADD COLUMN business_hours_json TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE conversations ADD COLUMN last_auto_reply_epoch INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Issue 1: Conversation source + review flag
                db.execSQL("ALTER TABLE conversations ADD COLUMN source TEXT NOT NULL DEFAULT 'SMS'")
                db.execSQL("ALTER TABLE conversations ADD COLUMN needs_review INTEGER NOT NULL DEFAULT 0")

                // Issue 4: Invoice relational chain
                db.execSQL("ALTER TABLE invoices ADD COLUMN vehicle_id TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE invoices ADD COLUMN appointment_id TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE invoices ADD COLUMN invoice_number TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_invoices_vehicle_id ON invoices(vehicle_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_invoices_appointment_id ON invoices(appointment_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_invoices_invoice_number ON invoices(invoice_number)")

                // Issue 5: Attachments table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS attachments (
                        id TEXT NOT NULL PRIMARY KEY,
                        conversation_id TEXT NOT NULL,
                        message_id TEXT DEFAULT NULL,
                        job_id TEXT DEFAULT NULL,
                        file_uri TEXT NOT NULL,
                        file_name TEXT,
                        mime_type TEXT NOT NULL,
                        file_size_bytes INTEGER,
                        shared_at_epoch INTEGER NOT NULL,
                        thumbnail_uri TEXT,
                        FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
                        FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE SET NULL,
                        FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE SET NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_conversation_id ON attachments(conversation_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_message_id ON attachments(message_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_job_id ON attachments(job_id)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite can't add FKs via ALTER TABLE — must recreate
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS conversations_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        customer_id TEXT DEFAULT NULL,
                        phone_number TEXT NOT NULL,
                        display_name TEXT DEFAULT NULL,
                        last_message_epoch INTEGER DEFAULT NULL,
                        unread_count INTEGER NOT NULL DEFAULT 0,
                        source TEXT NOT NULL DEFAULT 'SMS',
                        needs_review INTEGER NOT NULL DEFAULT 0,
                        last_auto_reply_epoch INTEGER DEFAULT NULL,
                        job_id TEXT DEFAULT NULL,
                        appointment_id TEXT DEFAULT NULL,
                        FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
                        FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE SET NULL,
                        FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO conversations_new (id, customer_id, phone_number, display_name, 
                        last_message_epoch, unread_count, source, needs_review, last_auto_reply_epoch)
                    SELECT id, customer_id, phone_number, display_name,
                        last_message_epoch, unread_count, source, needs_review, last_auto_reply_epoch
                    FROM conversations
                """.trimIndent())
                db.execSQL("DROP TABLE conversations")
                db.execSQL("ALTER TABLE conversations_new RENAME TO conversations")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_customer_id ON conversations(customer_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_phone_number ON conversations(phone_number)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_job_id ON conversations(job_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_appointment_id ON conversations(appointment_id)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shop_profile ADD COLUMN parts_markup_mode TEXT NOT NULL DEFAULT 'SLIDING'")
                db.execSQL("ALTER TABLE shop_profile ADD COLUMN markup_tiers_json TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE shop_profile ADD COLUMN mileage_rate_cents INTEGER NOT NULL DEFAULT 70")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN receipt_uri TEXT DEFAULT NULL")
            }
        }
    }
}

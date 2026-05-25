package com.phillips.phill.data.database

import androidx.room.TypeConverter
import com.phillips.phill.domain.enums.AppointmentStatus
import com.phillips.phill.domain.enums.ExpenseCategory
import com.phillips.phill.domain.enums.InvoiceStatus
import com.phillips.phill.domain.enums.JobStatus
import com.phillips.phill.domain.enums.LineItemType
import com.phillips.phill.domain.enums.MessageStatus
import com.phillips.phill.domain.enums.PaymentMethod

class Converters {
    @TypeConverter fun fromJobStatus(value: JobStatus): String = value.name
    @TypeConverter fun toJobStatus(value: String): JobStatus = JobStatus.valueOf(value)

    @TypeConverter fun fromInvoiceStatus(value: InvoiceStatus): String = value.name
    @TypeConverter fun toInvoiceStatus(value: String): InvoiceStatus = InvoiceStatus.valueOf(value)

    @TypeConverter fun fromLineItemType(value: LineItemType): String = value.name
    @TypeConverter fun toLineItemType(value: String): LineItemType = LineItemType.valueOf(value)

    @TypeConverter fun fromPaymentMethod(value: PaymentMethod): String = value.name
    @TypeConverter fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)

    @TypeConverter fun fromExpenseCategory(value: ExpenseCategory): String = value.name
    @TypeConverter fun toExpenseCategory(value: String): ExpenseCategory = ExpenseCategory.valueOf(value)

    @TypeConverter fun fromAppointmentStatus(value: AppointmentStatus): String = value.name
    @TypeConverter fun toAppointmentStatus(value: String): AppointmentStatus = AppointmentStatus.valueOf(value)

    @TypeConverter fun fromMessageStatus(value: MessageStatus): String = value.name
    @TypeConverter fun toMessageStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
}

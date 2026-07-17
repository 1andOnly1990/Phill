package com.phillips.phill.domain.billing

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.phillips.phill.data.entity.CustomerEntity
import com.phillips.phill.data.entity.InvoiceEntity
import com.phillips.phill.data.entity.LineItemEntity
import com.phillips.phill.data.entity.ShopProfileEntity
import com.phillips.phill.data.entity.VehicleEntity
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Generates professional PDF invoices using Android's built-in PdfDocument API.
 * No external libraries required.
 *
 * Standard US Letter: 612 x 792 points (8.5" x 11" at 72 dpi)
 */
object InvoicePdfGenerator {

    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

    fun generate(
        context: Context,
        invoice: InvoiceEntity,
        lineItems: List<LineItemEntity>,
        customer: CustomerEntity?,
        vehicle: VehicleEntity?,
        shopProfile: ShopProfileEntity?
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = MARGIN

        // --- Paints ---
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            isAntiAlias = true
        }
        val boldBody = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val smallPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        val accentPaint = Paint().apply {
            color = Color.rgb(33, 150, 243) // Material Blue
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // --- Shop Header ---
        shopProfile?.let { profile ->
            profile.businessName?.let {
                canvas.drawText(it, MARGIN, y + 20f, accentPaint)
                y += 28f
            }
            profile.businessAddress?.let {
                canvas.drawText(it, MARGIN, y + 12f, bodyPaint)
                y += 16f
            }
            val ownerLine = listOfNotNull(profile.ownerName, profile.ownerPhone).joinToString(" • ")
            if (ownerLine.isNotEmpty()) {
                canvas.drawText(ownerLine, MARGIN, y + 12f, bodyPaint)
                y += 16f
            }
            profile.licenseNumber?.let {
                canvas.drawText("License: $it", MARGIN, y + 12f, smallPaint)
                y += 14f
            }
        }

        y += 8f
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, linePaint)
        y += 16f

        // --- Invoice Header (right-aligned details) ---
        val statusLabel = when (invoice.status) {
            com.phillips.phill.domain.enums.InvoiceStatus.ESTIMATE -> "ESTIMATE"
            com.phillips.phill.domain.enums.InvoiceStatus.INVOICE -> "INVOICE"
            com.phillips.phill.domain.enums.InvoiceStatus.PAID -> "INVOICE (PAID)"
            com.phillips.phill.domain.enums.InvoiceStatus.VOID -> "INVOICE (VOID)"
        }
        canvas.drawText(statusLabel, MARGIN, y + 14f, titlePaint)

        invoice.invoiceNumber?.let { num ->
            val numText = "#$num"
            canvas.drawText(numText, MARGIN + CONTENT_WIDTH - headerPaint.measureText(numText), y + 14f, headerPaint)
        }
        y += 22f

        val createdDate = Instant.ofEpochMilli(invoice.createdAtEpoch)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
        canvas.drawText("Date: $createdDate", MARGIN, y + 12f, bodyPaint)
        y += 20f

        // --- Customer / Vehicle ---
        canvas.drawText("Bill To:", MARGIN, y + 12f, headerPaint)
        y += 18f
        customer?.let { c ->
            canvas.drawText("${c.firstName} ${c.lastName}", MARGIN + 8f, y + 12f, boldBody)
            y += 16f
            canvas.drawText(c.phoneNumber, MARGIN + 8f, y + 12f, bodyPaint)
            y += 16f
            c.email?.let {
                canvas.drawText(it, MARGIN + 8f, y + 12f, bodyPaint)
                y += 16f
            }
            c.address?.let {
                canvas.drawText(it, MARGIN + 8f, y + 12f, bodyPaint)
                y += 16f
            }
        }

        vehicle?.let { v ->
            val vDesc = buildString {
                v.year?.let { append("$it ") }
                append("${v.make} ${v.model}")
                v.vin?.let { append(" • VIN: $it") }
            }
            canvas.drawText("Vehicle: $vDesc", MARGIN + 8f, y + 12f, bodyPaint)
            y += 16f
        }

        y += 8f
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, linePaint)
        y += 12f

        // --- Line Items Table ---
        // Headers
        val colType = MARGIN
        val colDesc = MARGIN + 60f
        val colQty = MARGIN + 320f
        val colPrice = MARGIN + 380f
        val colTotal = MARGIN + 470f

        canvas.drawText("Type", colType, y + 12f, boldBody)
        canvas.drawText("Description", colDesc, y + 12f, boldBody)
        canvas.drawText("Qty", colQty, y + 12f, boldBody)
        canvas.drawText("Unit Price", colPrice, y + 12f, boldBody)
        canvas.drawText("Total", colTotal, y + 12f, boldBody)
        y += 16f
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, linePaint)
        y += 8f

        for (item in lineItems) {
            if (y > PAGE_HEIGHT - 120f) break // Prevent overflow

            canvas.drawText(item.type.name, colType, y + 11f, smallPaint)
            val desc = if (item.description.length > 40) item.description.take(37) + "..." else item.description
            canvas.drawText(desc, colDesc, y + 11f, bodyPaint)
            canvas.drawText(BillingEngine.formatThousandths(item.quantityThousandths), colQty, y + 11f, bodyPaint)
            canvas.drawText(BillingEngine.formatCents(item.unitPriceCents), colPrice, y + 11f, bodyPaint)
            canvas.drawText(BillingEngine.formatCents(item.totalCents), colTotal, y + 11f, bodyPaint)
            y += 16f
        }

        y += 4f
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, linePaint)
        y += 12f

        // --- Totals ---
        val totalsX = MARGIN + 360f
        val totalsValX = MARGIN + 470f

        fun drawTotalLine(label: String, cents: Long, paint: Paint = bodyPaint) {
            canvas.drawText(label, totalsX, y + 12f, paint)
            canvas.drawText(BillingEngine.formatCents(cents), totalsValX, y + 12f, paint)
            y += 16f
        }

        drawTotalLine("Subtotal:", invoice.subtotalCents)
        if (invoice.serviceFeeCents > 0) {
            drawTotalLine("Service Fee:", invoice.serviceFeeCents)
        }
        if (invoice.taxCents > 0) {
            drawTotalLine("Tax:", invoice.taxCents)
        }

        y += 4f
        canvas.drawLine(totalsX, y, MARGIN + CONTENT_WIDTH, y, linePaint)
        y += 12f

        canvas.drawText("TOTAL:", totalsX, y + 14f, headerPaint)
        canvas.drawText(BillingEngine.formatCents(invoice.totalCents), totalsValX, y + 14f, headerPaint)
        y += 24f

        // --- Terms ---
        invoice.termsText?.let { terms ->
            y += 8f
            canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, linePaint)
            y += 12f
            canvas.drawText("Terms & Conditions:", MARGIN, y + 12f, boldBody)
            y += 16f
            // Word wrap terms text
            val words = terms.split(" ")
            var currentLine = ""
            for (word in words) {
                val test = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (bodyPaint.measureText(test) > CONTENT_WIDTH - 16f) {
                    canvas.drawText(currentLine, MARGIN + 8f, y + 11f, smallPaint)
                    y += 14f
                    currentLine = word
                } else {
                    currentLine = test
                }
            }
            if (currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, MARGIN + 8f, y + 11f, smallPaint)
                y += 14f
            }
        }

        // --- Footer ---
        canvas.drawText(
            "Thank you for your business!",
            MARGIN,
            (PAGE_HEIGHT - MARGIN - 10f),
            smallPaint
        )

        document.finishPage(page)

        // Save to app-private invoices directory
        val invoicesDir = File(context.filesDir, "invoices")
        invoicesDir.mkdirs()
        val fileName = "${invoice.invoiceNumber ?: invoice.id}.pdf"
        val file = File(invoicesDir, fileName)
        file.outputStream().use { document.writeTo(it) }
        document.close()

        return file
    }
}

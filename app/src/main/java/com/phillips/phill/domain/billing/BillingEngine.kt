package com.phillips.phill.domain.billing

import java.text.NumberFormat
import java.util.Locale

/**
 * Pure financial math engine. All calculations use integer cents (Long).
 * No floating-point currency math. Anchored in the Fiduciary Rule.
 *
 * Quantities are stored as thousandths (Long): 1.5 hours = 1500L
 * Rates are stored as basis points (Int): 6% = 600, 1.4x = 14000
 */
object BillingEngine {

    /**
     * Calculate line item total from quantity (thousandths) and unit price (cents).
     * Example: 1.5 hours (1500L) at $125/hr (12500L) = $187.50 (18750L)
     * Formula: qty * price / 1000
     */
    fun calculateLineItemTotal(quantityThousandths: Long, unitPriceCents: Long): Long {
        return quantityThousandths * unitPriceCents / 1000L
    }

    /**
     * Calculate tax on taxable subtotal using basis points.
     * Example: $63.00 (6300L) at 6% (600 bp) = $3.78 (378L)
     * Formula: subtotal * rate / 10000
     */
    fun calculateTax(taxableSubtotalCents: Long, taxRateBasisPoints: Int): Long {
        return taxableSubtotalCents * taxRateBasisPoints.toLong() / 10000L
    }

    /**
     * Apply parts markup using basis points.
     * Example: $45.00 cost (4500L) at 1.4x (14000 bp) = $63.00 (6300L)
     * Formula: cost * markup / 10000
     */
    fun applyPartsMarkup(costCents: Long, markupBasisPoints: Int): Long {
        return costCents * markupBasisPoints.toLong() / 10000L
    }

    /**
     * Calculate complete invoice totals from line items.
     * Tax applies ONLY to taxable line items (parts). Labor and misc are NOT taxed.
     * SC Code Regs. § 117-306.
     */
    fun calculateInvoiceTotal(
        lineItemTotals: List<LineItemTotal>,
        serviceFeeCents: Long,
        taxRateBasisPoints: Int
    ): InvoiceTotals {
        var laborSubtotal = 0L
        var partsSubtotal = 0L
        var miscSubtotal = 0L

        for (item in lineItemTotals) {
            when {
                item.isTaxable -> partsSubtotal += item.totalCents
                item.isLabor -> laborSubtotal += item.totalCents
                else -> miscSubtotal += item.totalCents
            }
        }

        val taxCents = calculateTax(partsSubtotal, taxRateBasisPoints)
        val grandTotal = laborSubtotal + partsSubtotal + miscSubtotal + serviceFeeCents + taxCents

        return InvoiceTotals(
            laborSubtotalCents = laborSubtotal,
            partsSubtotalCents = partsSubtotal,
            miscSubtotalCents = miscSubtotal,
            serviceFeeCents = serviceFeeCents,
            taxableSubtotalCents = partsSubtotal,
            taxCents = taxCents,
            grandTotalCents = grandTotal
        )
    }

    /** Format cents to display string: 12500L -> "$125.00" */
    fun formatCents(cents: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        return formatter.format(cents / 100.0)
    }

    /** Parse dollar string to cents: "125.00" -> 12500L. Returns null if invalid. */
    fun parseDollarsToCents(dollars: String): Long? {
        val cleaned = dollars.replace("$", "").replace(",", "").trim()
        val value = cleaned.toDoubleOrNull() ?: return null
        return Math.round(value * 100)
    }

    /** Format basis points to percentage string: 600 -> "6.00%" */
    fun formatBasisPoints(basisPoints: Int): String {
        val percent = basisPoints / 100.0
        return String.format(Locale.US, "%.2f%%", percent)
    }

    /** Parse percentage string to basis points: "6.0" -> 600. Returns null if invalid. */
    fun parsePercentToBasisPoints(percent: String): Int? {
        val cleaned = percent.replace("%", "").trim()
        val value = cleaned.toDoubleOrNull() ?: return null
        return Math.round(value * 100).toInt()
    }

    /** Format thousandths to display: 1500L -> "1.5" */
    fun formatThousandths(thousandths: Long): String {
        val value = thousandths / 1000.0
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }

    /** Parse hours string to thousandths: "1.5" -> 1500L */
    fun parseHoursToThousandths(hours: String): Long? {
        val value = hours.trim().toDoubleOrNull() ?: return null
        return Math.round(value * 1000)
    }
}

data class LineItemTotal(
    val totalCents: Long,
    val isTaxable: Boolean,
    val isLabor: Boolean
)

data class InvoiceTotals(
    val laborSubtotalCents: Long,
    val partsSubtotalCents: Long,
    val miscSubtotalCents: Long,
    val serviceFeeCents: Long,
    val taxableSubtotalCents: Long,
    val taxCents: Long,
    val grandTotalCents: Long
)

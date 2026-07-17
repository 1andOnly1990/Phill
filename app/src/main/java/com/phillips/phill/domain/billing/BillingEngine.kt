package com.phillips.phill.domain.billing

import java.text.NumberFormat
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Pure financial math engine. All calculations use integer cents (Long).
 * No floating-point currency math. Anchored in the Fiduciary Rule.
 *
 * Quantities are stored as thousandths (Long): 1.5 hours = 1500L
 * Rates are stored as basis points (Int): 6% = 600, 1.4x = 14000
 */
object BillingEngine {

    /**
     * Industry-standard sliding-scale parts markup tiers (NAPA/Worldpac).
     * Higher markup on cheap parts, lower markup on expensive parts.
     * upToCents = 0 means "and above" (catch-all for the last tier).
     */
    val DEFAULT_MARKUP_TIERS: List<MarkupTier> = listOf(
        MarkupTier(upToCents = 2500L,   markupBasisPoints = 20000),  // $0-$25:     100% markup (2.0x)
        MarkupTier(upToCents = 5000L,   markupBasisPoints = 18000),  // $25-$50:     80% markup (1.8x)
        MarkupTier(upToCents = 10000L,  markupBasisPoints = 16000),  // $50-$100:    60% markup (1.6x)
        MarkupTier(upToCents = 25000L,  markupBasisPoints = 14000),  // $100-$250:   40% markup (1.4x)
        MarkupTier(upToCents = 50000L,  markupBasisPoints = 13000),  // $250-$500:   30% markup (1.3x)
        MarkupTier(upToCents = 100000L, markupBasisPoints = 12000),  // $500-$1000:  20% markup (1.2x)
        MarkupTier(upToCents = 0L,      markupBasisPoints = 11500)   // $1000+:      15% markup (1.15x)
    )

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
     * Apply flat parts markup using basis points.
     * Example: $45.00 cost (4500L) at 1.4x (14000 bp) = $63.00 (6300L)
     * Formula: cost * markup / 10000
     */
    fun applyPartsMarkup(costCents: Long, markupBasisPoints: Int): Long {
        return costCents * markupBasisPoints.toLong() / 10000L
    }

    /**
     * Apply industry-standard sliding-scale parts markup.
     * The markup percentage decreases as the part cost increases.
     * Looks up the applicable tier based on cost, then applies that tier's multiplier.
     *
     * @param costCents the wholesale/cost price in cents
     * @param tiers ordered list of markup tiers (last tier with upToCents=0 is catch-all)
     * @return the retail price in cents after markup
     */
    fun applySlidingScaleMarkup(costCents: Long, tiers: List<MarkupTier> = DEFAULT_MARKUP_TIERS): Long {
        val tier = tiers.firstOrNull { it.upToCents > 0 && costCents <= it.upToCents }
            ?: tiers.lastOrNull()
            ?: return costCents  // Fallback: no markup
        return costCents * tier.markupBasisPoints.toLong() / 10000L
    }

    /**
     * Calculate complete invoice totals from line items.
     * Tax applies ONLY to taxable line items (parts). Labor and misc are NOT taxed.
     * SC Code Regs. § 117-306.
     *
     * @param markupMode "FLAT" or "SLIDING" — determines which markup to apply to parts
     * @param flatMarkupBP flat markup basis points (used when mode = "FLAT")
     * @param slidingTiers sliding scale tiers (used when mode = "SLIDING")
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

    // --- Mileage Deduction (IRS) ---

    /** IRS standard mileage rate for 2025: $0.70 per mile = 70 cents */
    const val IRS_MILEAGE_RATE_CENTS_2025 = 70

    /**
     * Calculate IRS mileage deduction for tax purposes.
     * @param miles total business miles driven
     * @param rateCentsPerMile IRS standard rate in cents (default 2025 rate)
     * @return deduction amount in cents
     */
    fun calculateMileageDeduction(miles: Double, rateCentsPerMile: Int = IRS_MILEAGE_RATE_CENTS_2025): Long {
        return Math.round(miles * rateCentsPerMile)
    }

    // --- Formatting ---

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

    /** Serialize markup tiers to JSON for DB storage */
    fun serializeTiers(tiers: List<MarkupTier>): String {
        return Json.encodeToString(tiers)
    }

    /** Deserialize markup tiers from JSON. Returns default tiers on failure. */
    fun deserializeTiers(json: String?): List<MarkupTier> {
        if (json.isNullOrBlank()) return DEFAULT_MARKUP_TIERS
        return try {
            Json.decodeFromString<List<MarkupTier>>(json)
        } catch (_: Exception) {
            DEFAULT_MARKUP_TIERS
        }
    }
}

/**
 * A single tier in the sliding-scale parts markup table.
 * @param upToCents max part cost (in cents) for this tier. 0 = catch-all "and above".
 * @param markupBasisPoints multiplier in basis points (e.g., 14000 = 1.4x = 40% markup)
 */
@Serializable
data class MarkupTier(
    val upToCents: Long,
    val markupBasisPoints: Int
)

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


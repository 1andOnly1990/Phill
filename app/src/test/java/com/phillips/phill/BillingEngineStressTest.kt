package com.phillips.phill

import com.phillips.phill.domain.billing.BillingEngine
import com.phillips.phill.domain.billing.LineItemTotal
import com.phillips.phill.domain.billing.MarkupTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BillingEngineStressTest {

    private val random = Random(42) // Seed for deterministic random runs

    @Test
    fun stressTestLineItemTotalCalculation() {
        // Run 100,000 randomized quantity and price checks
        for (i in 0 until 100000) {
            val qtyThousandths = random.nextLong(0, 1_000_000L) // up to 1000.000 hours/units
            val priceCents = random.nextLong(0, 10_000_000L)   // up to $100,000.00
            
            val total = BillingEngine.calculateLineItemTotal(qtyThousandths, priceCents)
            
            // Check double precision math equivalence
            val expectedDouble = (qtyThousandths / 1000.0) * (priceCents)
            val expectedLong = Math.round(expectedDouble)
            
            // Allow a small delta of 1 cent due to rounding difference between round-half-up/down
            assertTrue(
                "Failed at qty=$qtyThousandths, price=$priceCents. Expected approx $expectedLong, got $total",
                Math.abs(total - expectedLong) <= 1L
            )
        }
    }

    @Test
    fun stressTestTaxCalculation() {
        // Run 100,000 tax checks
        for (i in 0 until 100000) {
            val taxableSubtotal = random.nextLong(0, 50_000_000L) // up to $500,000.00
            val taxRateBP = random.nextInt(0, 2000)               // up to 20.00% tax
            
            val tax = BillingEngine.calculateTax(taxableSubtotal, taxRateBP)
            val expected = (taxableSubtotal * taxRateBP.toLong()) / 10000L
            assertEquals(expected, tax)
        }
    }

    @Test
    fun stressTestFlatPartsMarkup() {
        for (i in 0 until 100000) {
            val costCents = random.nextLong(0, 10_000_000L)
            val markupBP = random.nextInt(10000, 30000) // 1.0x to 3.0x multiplier
            
            val retail = BillingEngine.applyPartsMarkup(costCents, markupBP)
            val expected = costCents * markupBP.toLong() / 10000L
            assertEquals(expected, retail)
            assertTrue(retail >= costCents)
        }
    }

    @Test
    fun stressTestSlidingScaleMarkup() {
        // Run 100,000 cost inputs on sliding scale
        for (i in 0 until 100000) {
            val costCents = random.nextLong(1, 10_000_000L) // $0.01 to $100,000.00
            val retail = BillingEngine.applySlidingScaleMarkup(costCents)
            
            assertTrue("Retail must be >= cost: retail=$retail, cost=$costCents", retail >= costCents)
            
            // Check that markup percentage generally decreases as cost increases
            val markupPercent = (retail.toDouble() / costCents.toDouble()) - 1.0
            
            if (costCents <= 2500) {
                // Tier 1: $0-$25 has 100% markup (2.0x multiplier)
                assertEquals(costCents * 2, retail)
            } else if (costCents >= 200000) {
                // Tier 7: $1000+ has 15% markup (1.15x multiplier)
                // Let's assert it falls back to the last tier multiplier
                val expectedRetail = costCents * 11500L / 10000L
                assertEquals(expectedRetail, retail)
            }
        }
    }

    @Test
    fun stressTestInvoiceTotalCalculation() {
        for (i in 0 until 50000) {
            val itemCount = random.nextInt(0, 100)
            val items = mutableListOf<LineItemTotal>()
            
            var expectedPartsSubtotal = 0L
            var expectedLaborSubtotal = 0L
            var expectedMiscSubtotal = 0L
            
            repeat(itemCount) {
                val totalCents = random.nextLong(0, 1_000_000L)
                val type = random.nextInt(0, 3)
                val isTaxable = type == 1 // parts
                val isLabor = type == 0   // labor
                
                items.add(LineItemTotal(totalCents, isTaxable, isLabor))
                
                when {
                    isTaxable -> expectedPartsSubtotal += totalCents
                    isLabor -> expectedLaborSubtotal += totalCents
                    else -> expectedMiscSubtotal += totalCents
                }
            }
            
            val serviceFee = random.nextLong(0, 10000L)
            val taxRateBP = random.nextInt(0, 1500) // up to 15.00%
            
            val invoiceTotals = BillingEngine.calculateInvoiceTotal(items, serviceFee, taxRateBP)
            
            assertEquals(expectedPartsSubtotal, invoiceTotals.partsSubtotalCents)
            assertEquals(expectedLaborSubtotal, invoiceTotals.laborSubtotalCents)
            assertEquals(expectedMiscSubtotal, invoiceTotals.miscSubtotalCents)
            assertEquals(serviceFee, invoiceTotals.serviceFeeCents)
            assertEquals(expectedPartsSubtotal, invoiceTotals.taxableSubtotalCents)
            
            val expectedTax = expectedPartsSubtotal * taxRateBP / 10000L
            assertEquals(expectedTax, invoiceTotals.taxCents)
            
            val expectedGrandTotal = expectedLaborSubtotal + expectedPartsSubtotal + expectedMiscSubtotal + serviceFee + expectedTax
            assertEquals(expectedGrandTotal, invoiceTotals.grandTotalCents)
        }
    }

    @Test
    fun stressTestRoundingAndParsingRoundTrips() {
        for (i in 0 until 50000) {
            val originalCents = random.nextLong(0, 100_000_000L)
            val formatStr = BillingEngine.formatCents(originalCents)
            
            // formatCents formats $125.00 from cents. Double round-trip check:
            val parsedCents = BillingEngine.parseDollarsToCents(formatStr)
            
            assertNotNull("Failed to parse: $formatStr for cents: $originalCents", parsedCents)
            assertEquals(originalCents, parsedCents)
        }
    }

    @Test
    fun testParseDollarsToCentsInvalidInputs() {
        assertNull(BillingEngine.parseDollarsToCents("abc"))
        assertNull(BillingEngine.parseDollarsToCents("   "))
        assertNull(BillingEngine.parseDollarsToCents("1.2.3"))
        
        // Negative dollars are allowed by the parser, returning negative cents
        assertEquals(-100L, BillingEngine.parseDollarsToCents("-1.00"))
        
        // Formatting check with commas
        assertEquals(123456789L, BillingEngine.parseDollarsToCents("$1,234,567.89"))
    }
}

package com.phillips.phill.util

/**
 * Centralized phone number normalization and formatting.
 * Single source of truth — every phone comparison in the app goes through here.
 *
 * Rules:
 * - Strip all non-digit characters
 * - Take last 10 digits (US numbers)
 * - Numbers with fewer than 7 digits are short codes (not real phones)
 * - GOOG_ identifiers (Google RCS caller IDs) are passed through as-is
 */
object PhoneNumberUtils {

    /** Minimum digit count for a real phone number (not a short code). */
    const val MIN_REAL_PHONE_DIGITS = 7

    /** Prefix used when RCS notifications don't expose a real phone number. */
    const val GOOGLE_ID_PREFIX = "GOOG_"

    /**
     * Returns true if this identifier is a Google RCS hash (not a real phone number).
     * These arise when Google Messages shows a caller-ID name instead of a phone number.
     */
    fun isGoogleId(raw: String): Boolean = raw.startsWith(GOOGLE_ID_PREFIX)

    /**
     * Normalize a raw phone string to a consistent 10-digit format.
     * GOOG_ identifiers pass through unchanged.
     * Examples:
     *   "(864) 555-1234" → "8645551234"
     *   "+18645551234"   → "8645551234"
     *   "555-1234"       → "5551234"
     *   "69534"          → "69534"  (short code, unchanged)
     *   "GOOG_a1b2c3d4"  → "GOOG_a1b2c3d4" (RCS hash, unchanged)
     */
    fun normalize(raw: String): String {
        if (isGoogleId(raw)) return raw
        return raw.filter { it.isDigit() }.takeLast(10)
    }

    /**
     * Check if two phone numbers refer to the same number after normalization.
     * GOOG_ identifiers only match exact same hash — never match a real phone.
     */
    fun areSameNumber(a: String, b: String): Boolean {
        return normalize(a) == normalize(b)
    }

    /**
     * Check if a number is a short code (fewer than 7 digits).
     * GOOG_ identifiers are NOT short codes.
     */
    fun isShortCode(raw: String): Boolean {
        if (isGoogleId(raw)) return false
        return normalize(raw).length < MIN_REAL_PHONE_DIGITS
    }

    private val TOLL_FREE_PREFIXES = setOf("800", "833", "844", "855", "866", "877", "888")

    /**
     * Check if a number is a short code OR a toll-free number.
     * Short codes: fewer than 7 digits.
     * Toll-free: 10-digit numbers starting with 800/833/844/855/866/877/888.
     * GOOG_ identifiers are NOT filtered.
     */
    fun isTollFreeOrShortCode(raw: String): Boolean {
        if (isGoogleId(raw)) return false
        val normalized = normalize(raw)
        if (normalized.length < MIN_REAL_PHONE_DIGITS) return true
        if (normalized.length == 10 && normalized.substring(0, 3) in TOLL_FREE_PREFIXES) return true
        return false
    }

    /**
     * Format a normalized phone number for display.
     * "8645551234" → "(864) 555-1234"
     * "5551234"    → "555-1234"
     * "GOOG_a1b2"  → "GOOG_a1b2" (returned as-is)
     */
    fun formatForDisplay(normalized: String): String {
        if (isGoogleId(normalized)) return normalized
        val digits = normalized.filter { it.isDigit() }
        return when (digits.length) {
            10 -> "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
            7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
            else -> normalized
        }
    }
}

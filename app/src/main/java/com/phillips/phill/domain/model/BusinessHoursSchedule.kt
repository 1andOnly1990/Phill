package com.phillips.phill.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the full weekly business hours schedule.
 * Stored as JSON in ShopProfileEntity.businessHoursJson.
 * Index 0 = Sunday, Index 1 = Monday, ..., Index 6 = Saturday.
 * This matches Calendar.DAY_OF_WEEK - 1.
 */
@Serializable
data class BusinessHoursSchedule(
    val days: List<DaySchedule> = defaultSchedule()
) {
    companion object {
        fun defaultSchedule(): List<DaySchedule> = listOf(
            DaySchedule(isOpen = false, openHHMM = "08:00", closeHHMM = "17:00"), // Sunday
            DaySchedule(isOpen = true,  openHHMM = "08:00", closeHHMM = "17:00"), // Monday
            DaySchedule(isOpen = true,  openHHMM = "08:00", closeHHMM = "17:00"), // Tuesday
            DaySchedule(isOpen = true,  openHHMM = "08:00", closeHHMM = "17:00"), // Wednesday
            DaySchedule(isOpen = true,  openHHMM = "08:00", closeHHMM = "17:00"), // Thursday
            DaySchedule(isOpen = true,  openHHMM = "08:00", closeHHMM = "17:00"), // Friday
            DaySchedule(isOpen = false, openHHMM = "08:00", closeHHMM = "12:00"), // Saturday
        )
    }
}

@Serializable
data class DaySchedule(
    val isOpen: Boolean,
    val openHHMM: String,  // 24-hour format string, e.g. "08:00" means 8:00 AM
    val closeHHMM: String  // 24-hour format string, e.g. "17:00" means 5:00 PM
) {
    /** Returns minutes since midnight for the open time. Returns 480 (8 AM) if format is invalid. */
    fun openMinutes(): Int = hhmmToMinutes(openHHMM) ?: 480

    /** Returns minutes since midnight for the close time. Returns 1020 (5 PM) if format is invalid. */
    fun closeMinutes(): Int = hhmmToMinutes(closeHHMM) ?: 1020
}

/**
 * Converts a "HH:MM" string to total minutes since midnight.
 * Returns null if the string is not in valid HH:MM format.
 * Examples: "08:00" -> 480, "17:30" -> 1050, "23:59" -> 1439
 */
fun hhmmToMinutes(hhmm: String): Int? {
    val parts = hhmm.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

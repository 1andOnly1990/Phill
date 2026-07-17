package com.phillips.phill.domain.extraction

/**
 * Aggregated result of data extraction from one or more customer messages.
 *
 * @property vehicles Vehicles identified in the text (year/make/model).
 * @property names Customer names parsed from self-introductions.
 * @property symptoms Automotive symptoms or complaints detected.
 * @property appointmentIntent Whether the customer appears to be requesting an appointment.
 * @property rawAppointmentText The original text fragment that triggered appointment-intent detection.
 */
data class ExtractionResult(
    val vehicles: List<ExtractedVehicle> = emptyList(),
    val names: List<ExtractedName> = emptyList(),
    val symptoms: List<ExtractedSymptom> = emptyList(),
    val appointmentIntent: Boolean = false,
    val rawAppointmentText: String? = null
)

/**
 * A vehicle reference extracted from message text.
 *
 * @property year Model year (nullable when matched via "my [make] [model]" without a year).
 * @property make Manufacturer / brand name.
 * @property model Model name or identifier.
 * @property raw The original matched substring from the message.
 */
data class ExtractedVehicle(
    val year: Int?,
    val make: String,
    val model: String,
    val raw: String
)

/**
 * A customer name extracted from a self-introduction phrase.
 *
 * @property firstName The customer's first name.
 * @property lastName The customer's last name, if a second capitalised word followed.
 * @property raw The original matched substring from the message.
 */
data class ExtractedName(
    val firstName: String,
    val lastName: String? = null,
    val raw: String
)

/**
 * An automotive symptom or complaint extracted from message text.
 *
 * @property text The keyword or phrase that was matched.
 * @property category The [SymptomCategory] this symptom maps to.
 */
data class ExtractedSymptom(
    val text: String,
    val category: SymptomCategory
)

/**
 * High-level categories for automotive symptoms and service requests.
 */
enum class SymptomCategory {
    ENGINE,
    BRAKES,
    SUSPENSION,
    ELECTRICAL,
    FLUID_LEAK,
    TIRE,
    HVAC,
    BODY,
    TRANSMISSION,
    MAINTENANCE,
    OTHER
}

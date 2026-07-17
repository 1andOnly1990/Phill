package com.phillips.phill.domain.extraction

/**
 * Pure-Kotlin singleton that extracts structured automotive-shop data from raw
 * customer message text.
 *
 * All regex patterns are compiled once as [companion object] constants so they
 * are never recreated per call. Every match is case-insensitive.
 *
 * ### Supported extractions
 * | Data            | Example input                                      |
 * |-----------------|----------------------------------------------------|
 * | Vehicle         | "I have a 2022 Toyota Tacoma"                      |
 * | Vehicle (no yr) | "my Ford F150"                                     |
 * | Name            | "Hi I'm John Smith"                                |
 * | Symptom         | "check engine light is on and brakes are squeaking" |
 * | Appointment     | "can I schedule an appointment for tomorrow?"       |
 */
object MessageDataExtractor {

    // ──────────────────────────────────────────────────────────────────────
    //  Vehicle extraction
    // ──────────────────────────────────────────────────────────────────────

    /** Recognised vehicle makes (lower-cased for comparison). */
    private val KNOWN_MAKES: Set<String> = setOf(
        "toyota", "honda", "ford", "chevy", "chevrolet", "gmc", "dodge",
        "ram", "jeep", "nissan", "hyundai", "kia", "subaru", "bmw",
        "mercedes", "audi", "volkswagen", "vw", "lexus", "acura",
        "infiniti", "mazda", "mitsubishi", "volvo", "buick", "cadillac",
        "lincoln", "chrysler", "tesla"
    )

    /** Matches "2022 Toyota Tacoma" style references. */
    private val YEAR_MAKE_MODEL_REGEX =
        Regex("""((?:19|20)\d{2})\s+([A-Za-z]+)\s+([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)

    /** Matches "my Toyota Tacoma" style references (no year). */
    private val MY_MAKE_MODEL_REGEX =
        Regex("""\bmy\s+([A-Za-z]+)\s+([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)

    // ──────────────────────────────────────────────────────────────────────
    //  Name extraction
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Matches self-introduction phrases and captures the name portion.
     *
     * Groups:
     * - 1 → first name
     * - 2 → optional last name (capitalised word following)
     */
    private val NAME_INTRO_REGEX = Regex(
        """(?:this\s+is|my\s+name\s+is|i'?\s*m|hi\s+i'?\s*m|hey\s+this\s+is)\s+([A-Z][a-z]+)(?:\s+([A-Z][a-z]+))?""",
        RegexOption.IGNORE_CASE
    )

    /** Common words that look like names but aren't — skip these. */
    private val NON_NAME_WORDS: Set<String> = setOf(
        "here", "ready", "looking", "having", "calling", "writing",
        "texting", "going", "trying", "interested", "wondering",
        "hoping", "back", "sure", "sorry", "happy", "glad",
        "just", "also", "still", "really", "very", "not", "the"
    )

    // ──────────────────────────────────────────────────────────────────────
    //  Symptom extraction
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Keyword phrases mapped to their [SymptomCategory].
     *
     * Order matters: longer / more-specific phrases are listed first so they
     * match before shorter substrings (e.g. "oil leak" before "oil").
     */
    private val SYMPTOM_KEYWORDS: List<Pair<String, SymptomCategory>> = listOf(
        // ENGINE
        "won't start"      to SymptomCategory.ENGINE,
        "check engine"     to SymptomCategory.ENGINE,
        "engine light"     to SymptomCategory.ENGINE,
        "misfire"          to SymptomCategory.ENGINE,
        "overheating"      to SymptomCategory.ENGINE,
        "knocking"         to SymptomCategory.ENGINE,
        "rough idle"       to SymptomCategory.ENGINE,
        "stalling"         to SymptomCategory.ENGINE,
        "no power"         to SymptomCategory.ENGINE,
        // BRAKES
        "braking"          to SymptomCategory.BRAKES,
        "brake"            to SymptomCategory.BRAKES,
        "squeaking"        to SymptomCategory.BRAKES,
        "grinding"         to SymptomCategory.BRAKES,
        "stopping"         to SymptomCategory.BRAKES,
        "pedal"            to SymptomCategory.BRAKES,
        // SUSPENSION
        "alignment"        to SymptomCategory.SUSPENSION,
        "pulling"          to SymptomCategory.SUSPENSION,
        "vibrating"        to SymptomCategory.SUSPENSION,
        "shaking"          to SymptomCategory.SUSPENSION,
        "steering"         to SymptomCategory.SUSPENSION,
        "strut"            to SymptomCategory.SUSPENSION,
        "shock"            to SymptomCategory.SUSPENSION,
        // ELECTRICAL
        "battery"          to SymptomCategory.ELECTRICAL,
        "alternator"       to SymptomCategory.ELECTRICAL,
        "won't charge"     to SymptomCategory.ELECTRICAL,
        "lights"           to SymptomCategory.ELECTRICAL,
        "fuse"             to SymptomCategory.ELECTRICAL,
        // FLUID_LEAK
        "coolant leak"     to SymptomCategory.FLUID_LEAK,
        "oil leak"         to SymptomCategory.FLUID_LEAK,
        "leaking"          to SymptomCategory.FLUID_LEAK,
        "dripping"         to SymptomCategory.FLUID_LEAK,
        "fluid"            to SymptomCategory.FLUID_LEAK,
        // TIRE
        "flat tire"        to SymptomCategory.TIRE,
        "tire"             to SymptomCategory.TIRE,
        "tyre"             to SymptomCategory.TIRE,
        "wheel"            to SymptomCategory.TIRE,
        "rotation"         to SymptomCategory.TIRE,
        // HVAC
        "air conditioning" to SymptomCategory.HVAC,
        "a/c"              to SymptomCategory.HVAC,
        "ac"               to SymptomCategory.HVAC,
        "heater"           to SymptomCategory.HVAC,
        "heat"             to SymptomCategory.HVAC,
        "blowing hot"      to SymptomCategory.HVAC,
        "blowing cold"     to SymptomCategory.HVAC,
        // TRANSMISSION
        "transmission"     to SymptomCategory.TRANSMISSION,
        "shifting"         to SymptomCategory.TRANSMISSION,
        "slipping"         to SymptomCategory.TRANSMISSION,
        "gear"             to SymptomCategory.TRANSMISSION,
        // MAINTENANCE
        "oil change"       to SymptomCategory.MAINTENANCE,
        "tune up"          to SymptomCategory.MAINTENANCE,
        "state inspection" to SymptomCategory.MAINTENANCE,
        "maintenance"      to SymptomCategory.MAINTENANCE,
        "service"          to SymptomCategory.MAINTENANCE,
        "inspection"       to SymptomCategory.MAINTENANCE
    )

    /** Pre-compiled regex for each symptom keyword (word-boundary aware where possible). */
    private val SYMPTOM_PATTERNS: List<Pair<Regex, SymptomCategory>> =
        SYMPTOM_KEYWORDS.map { (keyword, category) ->
            Regex("""\b${Regex.escape(keyword)}\b""", RegexOption.IGNORE_CASE) to category
        }

    // ──────────────────────────────────────────────────────────────────────
    //  Appointment intent detection
    // ──────────────────────────────────────────────────────────────────────

    /** Keywords that signal an appointment or scheduling request. */
    private val APPOINTMENT_KEYWORDS: List<String> = listOf(
        "appointment", "schedule", "available", "come by", "drop off",
        "bring in", "book", "when can", "time slot", "tomorrow",
        "next week", "this week", "today"
    )

    /** Pre-compiled patterns for appointment keywords. */
    private val APPOINTMENT_PATTERNS: List<Regex> =
        APPOINTMENT_KEYWORDS.map { keyword ->
            Regex("""\b${Regex.escape(keyword)}\b""", RegexOption.IGNORE_CASE)
        }

    // ──────────────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Extracts all recognisable data from a single message body.
     *
     * @param messageBody The raw text of one SMS / chat message.
     * @return An [ExtractionResult] containing every extraction found.
     */
    fun extract(messageBody: String): ExtractionResult {
        val vehicles = extractVehicles(messageBody)
        val names = extractNames(messageBody)
        val symptoms = extractSymptoms(messageBody)
        val (appointmentIntent, rawAppointmentText) = detectAppointmentIntent(messageBody)

        return ExtractionResult(
            vehicles = vehicles,
            names = names,
            symptoms = symptoms,
            appointmentIntent = appointmentIntent,
            rawAppointmentText = rawAppointmentText
        )
    }

    /**
     * Extracts data across an entire conversation, merging and deduplicating
     * results from every message.
     *
     * @param messages Ordered list of raw message bodies (oldest → newest).
     * @return A single [ExtractionResult] with deduplicated vehicles, names,
     *         and symptoms. Appointment intent is `true` if *any* message
     *         triggered it.
     */
    fun extractFromConversation(messages: List<String>): ExtractionResult {
        val allVehicles = mutableListOf<ExtractedVehicle>()
        val allNames = mutableListOf<ExtractedName>()
        val allSymptoms = mutableListOf<ExtractedSymptom>()
        var appointmentIntent = false
        var rawAppointmentText: String? = null

        for (message in messages) {
            val result = extract(message)
            allVehicles.addAll(result.vehicles)
            allNames.addAll(result.names)
            allSymptoms.addAll(result.symptoms)
            if (result.appointmentIntent && !appointmentIntent) {
                appointmentIntent = true
                rawAppointmentText = result.rawAppointmentText
            }
        }

        return ExtractionResult(
            vehicles = deduplicateVehicles(allVehicles),
            names = deduplicateNames(allNames),
            symptoms = deduplicateSymptoms(allSymptoms),
            appointmentIntent = appointmentIntent,
            rawAppointmentText = rawAppointmentText
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Internal helpers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Finds vehicles in [text] using both year-make-model and "my make model"
     * patterns, validating the make against [KNOWN_MAKES].
     */
    private fun extractVehicles(text: String): List<ExtractedVehicle> {
        val vehicles = mutableListOf<ExtractedVehicle>()

        // Year + Make + Model  (e.g. "2022 Toyota Tacoma")
        YEAR_MAKE_MODEL_REGEX.findAll(text).forEach { match ->
            val make = match.groupValues[2]
            if (make.lowercase() in KNOWN_MAKES) {
                vehicles.add(
                    ExtractedVehicle(
                        year = match.groupValues[1].toIntOrNull(),
                        make = make,
                        model = match.groupValues[3],
                        raw = match.value.trim()
                    )
                )
            }
        }

        // "my [Make] [Model]"  (e.g. "my Ford F150")
        MY_MAKE_MODEL_REGEX.findAll(text).forEach { match ->
            val make = match.groupValues[1]
            if (make.lowercase() in KNOWN_MAKES) {
                // Avoid duplicating a vehicle already captured with a year
                val alreadyCaptured = vehicles.any {
                    it.make.equals(make, ignoreCase = true) &&
                            it.model.equals(match.groupValues[2], ignoreCase = true)
                }
                if (!alreadyCaptured) {
                    vehicles.add(
                        ExtractedVehicle(
                            year = null,
                            make = make,
                            model = match.groupValues[2],
                            raw = match.value.trim()
                        )
                    )
                }
            }
        }

        return vehicles
    }

    /**
     * Finds customer names in self-introduction phrases.
     * Skips words that appear in [NON_NAME_WORDS].
     */
    private fun extractNames(text: String): List<ExtractedName> {
        val names = mutableListOf<ExtractedName>()

        NAME_INTRO_REGEX.findAll(text).forEach { match ->
            val firstName = match.groupValues[1]
            if (firstName.lowercase() !in NON_NAME_WORDS) {
                val lastName = match.groupValues[2].ifBlank { null }
                names.add(
                    ExtractedName(
                        firstName = firstName,
                        lastName = lastName,
                        raw = match.value.trim()
                    )
                )
            }
        }

        return names
    }

    /**
     * Scans [text] for known automotive symptom keywords and maps each match
     * to its [SymptomCategory].
     */
    private fun extractSymptoms(text: String): List<ExtractedSymptom> {
        val symptoms = mutableListOf<ExtractedSymptom>()
        val alreadyMatched = mutableSetOf<String>()

        for ((pattern, category) in SYMPTOM_PATTERNS) {
            pattern.findAll(text).forEach { match ->
                val matchedText = match.value.lowercase()
                if (matchedText !in alreadyMatched) {
                    alreadyMatched.add(matchedText)
                    symptoms.add(
                        ExtractedSymptom(
                            text = match.value,
                            category = category
                        )
                    )
                }
            }
        }

        return symptoms
    }

    /**
     * Checks whether [text] contains any appointment-related keywords.
     *
     * @return A [Pair] of (intentDetected, firstMatchedFragment).
     */
    private fun detectAppointmentIntent(text: String): Pair<Boolean, String?> {
        for (pattern in APPOINTMENT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                return true to match.value
            }
        }
        return false to null
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Deduplication helpers
    // ──────────────────────────────────────────────────────────────────────

    /** Deduplicates vehicles by (make, model) pair, case-insensitive. */
    private fun deduplicateVehicles(vehicles: List<ExtractedVehicle>): List<ExtractedVehicle> {
        val seen = mutableSetOf<String>()
        return vehicles.filter { v ->
            val key = "${v.make.lowercase()}|${v.model.lowercase()}"
            seen.add(key)
        }
    }

    /** Deduplicates names by first name, case-insensitive. */
    private fun deduplicateNames(names: List<ExtractedName>): List<ExtractedName> {
        val seen = mutableSetOf<String>()
        return names.filter { n ->
            val key = n.firstName.lowercase()
            seen.add(key)
        }
    }

    /** Deduplicates symptoms by (lowercased text, category) pair. */
    private fun deduplicateSymptoms(symptoms: List<ExtractedSymptom>): List<ExtractedSymptom> {
        val seen = mutableSetOf<String>()
        return symptoms.filter { s ->
            val key = "${s.text.lowercase()}|${s.category}"
            seen.add(key)
        }
    }
}

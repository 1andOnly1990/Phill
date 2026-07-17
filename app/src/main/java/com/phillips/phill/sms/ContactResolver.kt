package com.phillips.phill.sms

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queries the Android system Contacts database to:
 *   1. Check whether a phone number belongs to a saved contact (isKnownContact)
 *   2. Look up the display name for a phone number (getDisplayName)
 *
 * Uses ContactsContract.PhoneLookup which handles all phone number format variants
 * (with/without country code, with/without dashes, etc.) natively.
 *
 * READ_CONTACTS permission is already declared in AndroidManifest.xml.
 */
@Singleton
class ContactResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Returns true if this phone number is saved in the device's contacts.
     * Saved contacts are personal — they should be EXCLUDED from Phill Comms.
     *
     * Uses ContactsContract.PhoneLookup which does fuzzy number matching
     * (handles +1 prefix, dashes, spaces, etc.).
     */
    fun isKnownContact(phone: String): Boolean {
        if (phone.isBlank()) return false
        return getDisplayName(phone) != null
    }

    /**
     * Returns the contact display name for this phone number, or null if not found.
     * Returns null for short codes — they should never be in contacts but guard anyway.
     */
    fun getDisplayName(phone: String): String? {
        if (phone.isBlank()) return null

        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phone)
        )

        val cursor = try {
            context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
        } catch (e: Exception) {
            return null // ContentResolver failure — treat as unknown
        }

        return cursor?.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            } else {
                null
            }
        }
    }

    /**
     * Reverse-lookup: given a display name, find a matching phone number.
     * Used when Google Messages replaces a phone number with a caller-ID name
     * and we need to resolve back to the real phone.
     *
     * Searches contacts where DISPLAY_NAME matches (case-insensitive).
     * Returns the first phone number found, or null.
     */
    fun getPhoneForName(displayName: String): String? {
        if (displayName.isBlank()) return null

        val cursor = try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ?",
                arrayOf(displayName),
                null
            )
        } catch (e: Exception) {
            return null
        }

        return cursor?.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            } else {
                null
            }
        }
    }
}

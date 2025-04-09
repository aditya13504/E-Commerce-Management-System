package com.yourdomain.ecommerce.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility class for date and time operations compatible with lower API levels
 */
object DateTimeUtil {
    /**
     * Returns the current timestamp as an ISO 8601 formatted string.
     * This is API level compatible replacement for Instant.now().toString()
     */
    fun getCurrentTimestampString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date())
    }
    
    /**
     * Returns a timestamp for a future time by adding seconds to the current time
     */
    fun getFutureTimestampString(secondsToAdd: Long): String {
        val date = Date(System.currentTimeMillis() + (secondsToAdd * 1000))
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(date)
    }
} 
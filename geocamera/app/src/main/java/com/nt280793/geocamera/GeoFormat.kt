package com.nt280793.geocamera

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object GeoFormat {
    fun latitude(value: Double): String = hemisphere(value, "N", "S")

    fun longitude(value: Double): String = hemisphere(value, "E", "W")

    fun latitudeDms(value: Double): String = dms(value, "N", "S")

    fun longitudeDms(value: Double): String = dms(value, "E", "W")

    fun formatWhen(
        epochMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm:ss", locale)
        return Instant.ofEpochMilli(epochMillis).atZone(zone).format(fmt)
    }

    fun exifDateTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val fmt = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US)
        return Instant.ofEpochMilli(epochMillis).atZone(zone).format(fmt)
    }

    fun fileStamp(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val fmt = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS", Locale.US)
        return Instant.ofEpochMilli(epochMillis).atZone(zone).format(fmt)
    }

    fun prettyExifDate(raw: String?, locale: Locale = Locale.getDefault()): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val parsed = LocalDateTime.parse(
                raw.trim(),
                DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US)
            )
            parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm:ss", locale))
        } catch (_: Exception) {
            raw
        }
    }

    fun rationalToDouble(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.trim().split("/")
        val numerator = parts[0].toDoubleOrNull() ?: return null
        val denominator = parts.getOrNull(1)?.toDoubleOrNull() ?: 1.0
        if (denominator == 0.0) return null
        return numerator / denominator
    }

    private fun hemisphere(value: Double, positive: String, negative: String): String {
        val hemi = if (value >= 0.0) positive else negative
        return String.format(Locale.US, "%.6f° %s", abs(value), hemi)
    }

    private fun dms(value: Double, positive: String, negative: String): String {
        val hemi = if (value >= 0.0) positive else negative
        var degrees = abs(value).toInt()
        val minutesFull = (abs(value) - degrees) * 60.0
        var minutes = minutesFull.toInt()
        var seconds = (minutesFull - minutes) * 60.0
        if (seconds >= 59.95) {
            seconds = 0.0
            minutes += 1
        }
        if (minutes >= 60) {
            minutes = 0
            degrees += 1
        }
        return String.format(Locale.US, "%d° %02d' %04.1f\" %s", degrees, minutes, seconds, hemi)
    }
}

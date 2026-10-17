package com.nt280793.geocamera

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.util.Locale
import kotlin.math.roundToInt

object PhotoCaption {
    fun lines(context: Context, uri: Uri): List<String> {
        val exif = try {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        } catch (_: Exception) {
            null
        } ?: return listOf(context.getString(R.string.no_metadata))
        val lines = mutableListOf<String>()
        GeoFormat.prettyExifDate(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL), Locale.getDefault())
            ?.let { lines += it }
        val latLong = exif.latLong
        if (latLong != null && latLong.size >= 2) {
            lines += "${GeoFormat.latitude(latLong[0])}   ${GeoFormat.longitude(latLong[1])}"
        }
        val extras = mutableListOf<String>()
        val altitude = exif.getAltitude(Double.NaN)
        if (!altitude.isNaN()) extras += "Alt ${altitude.roundToInt()} m"
        GeoFormat.rationalToDouble(exif.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION))
            ?.let { extras += "HDG ${it.roundToInt()}°" }
        if (extras.isNotEmpty()) lines += extras.joinToString("   ")
        exif.getAttribute(ExifInterface.TAG_USER_COMMENT)?.trim()?.takeIf { it.isNotEmpty() }?.let { lines += it }
        val description = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)?.trim().orEmpty()
        val decoded = SiteRecord.decode(description)
        if (decoded.recognized) {
            lines += decoded.site.lines(useDms = false)
            decoded.note.trim().takeIf { it.isNotEmpty() }?.let { lines += it }
        } else if (description.isNotEmpty()) {
            lines += description
        }
        return lines.ifEmpty { listOf(context.getString(R.string.no_metadata)) }
    }

    fun captureLines(context: Context, uri: Uri): List<String> {
        val exif = try {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        } catch (_: Exception) {
            null
        } ?: return emptyList()
        val lines = mutableListOf<String>()
        GeoFormat.prettyExifDate(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL), Locale.getDefault())
            ?.let { lines += it }
        val latLong = exif.latLong
        if (latLong != null && latLong.size >= 2) {
            lines += "${GeoFormat.latitude(latLong[0])}   ${GeoFormat.longitude(latLong[1])}"
        }
        val extras = mutableListOf<String>()
        val altitude = exif.getAltitude(Double.NaN)
        if (!altitude.isNaN()) extras += "Alt ${altitude.roundToInt()} m"
        GeoFormat.rationalToDouble(exif.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION))
            ?.let { extras += "HDG ${it.roundToInt()}°" }
        if (extras.isNotEmpty()) lines += extras.joinToString("   ")
        exif.getAttribute(ExifInterface.TAG_USER_COMMENT)?.trim()?.takeIf { it.isNotEmpty() }?.let { lines += it }
        return lines
    }

    fun readSite(context: Context, uri: Uri): DecodedSite {
        val exif = try {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        } catch (_: Exception) {
            null
        } ?: return DecodedSite(SiteRecord(), "", false)
        val description = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)?.trim().orEmpty()
        val decoded = SiteRecord.decode(description)
        if (!decoded.recognized && description.isNotEmpty()) {
            return DecodedSite(SiteRecord(), description, false)
        }
        return decoded
    }
}

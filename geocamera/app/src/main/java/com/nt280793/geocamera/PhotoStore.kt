package com.nt280793.geocamera

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.roundToInt

object PhotoStore {
    fun save(context: Context, jpeg: ByteArray, whenMillis: Long, info: StampInfo): Uri? {
        val name = "GEO_${GeoFormat.fileStamp(whenMillis)}.jpg"
        return if (Build.VERSION.SDK_INT >= 29) {
            saveScoped(context, jpeg, name, info, whenMillis)
        } else {
            saveLegacy(context, jpeg, name, info, whenMillis)
        }
    }

    fun list(context: Context): List<Uri> {
        val stored = query(context)
        val extra = CaptureIndex(context).all().filter { candidate ->
            stored.none { it == candidate }
        }
        return stored + extra
    }

    fun delete(context: Context, uri: Uri): Boolean {
        return try {
            if (uri.scheme == "file") {
                File(uri.path ?: return false).delete()
            } else {
                context.contentResolver.delete(uri, null, null) > 0
            }
        } catch (_: Exception) {
            false
        }
    }

    fun displayName(context: Context, uri: Uri): String {
        if (uri.scheme == "file") return uri.lastPathSegment ?: "Photo"
        return try {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: "Photo"
        } catch (_: Exception) {
            "Photo"
        }
    }

    private fun saveScoped(
        context: Context,
        jpeg: ByteArray,
        name: String,
        info: StampInfo,
        whenMillis: Long
    ): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GeoCamera")
            put(MediaStore.Images.Media.IS_PENDING, 1)
            put(MediaStore.Images.Media.DATE_TAKEN, whenMillis)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { it.write(jpeg) } ?: error("stream")
            resolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
                writeExif(ExifInterface(descriptor.fileDescriptor), info, whenMillis)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(
        context: Context,
        jpeg: ByteArray,
        name: String,
        info: StampInfo,
        whenMillis: Long
    ): Uri? {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "GeoCamera"
        )
        if (!dir.exists() && !dir.mkdirs()) return null
        val file = File(dir, name)
        return try {
            file.writeBytes(jpeg)
            writeExif(ExifInterface(file.absolutePath), info, whenMillis)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.DATE_TAKEN, whenMillis)
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }

    private fun writeExif(exif: ExifInterface, info: StampInfo, whenMillis: Long) {
        val lat = info.latitude
        val lon = info.longitude
        if (lat != null && lon != null) exif.setLatLong(lat, lon)
        info.altitudeM?.let { altitude ->
            exif.setAltitude(altitude)
        }
        info.headingDeg?.let { heading ->
            var degrees = heading.roundToInt() % 360
            if (degrees < 0) degrees += 360
            exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, "$degrees/1")
            exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M")
        }
        val taken = GeoFormat.exifDateTime(whenMillis, info.zone)
        exif.setAttribute(ExifInterface.TAG_DATETIME, taken)
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, taken)
        info.note?.trim()?.takeIf { it.isNotEmpty() }?.let { note ->
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, note)
        }
        exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Geo Camera")
        exif.saveAttributes()
    }

    private fun query(context: Context): List<Uri> {
        return try {
            val resolver = context.contentResolver
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED
            )
            val (selection, args) = if (Build.VERSION.SDK_INT >= 29) {
                "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?" to
                    arrayOf("Pictures/GeoCamera%", "GEO_%")
            } else {
                @Suppress("DEPRECATION")
                "${MediaStore.Images.Media.DATA} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?" to
                    arrayOf("%/GeoCamera/%", "GEO_%")
            }
            val found = mutableListOf<Pair<Long, Uri>>()
            resolver.query(
                collection,
                projection,
                selection,
                args,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val uri = ContentUris.withAppendedId(collection, cursor.getLong(idColumn))
                    found += cursor.getLong(dateColumn) to uri
                }
            }
            found.sortedByDescending { it.first }.map { it.second }.distinct()
        } catch (_: Exception) {
            emptyList()
        }
    }
}

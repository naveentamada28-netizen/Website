package com.nt280793.geocamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File

object PhotoProcessor {
    fun process(context: Context, source: File, info: StampInfo): Uri? {
        val decoded = decode(source) ?: return null
        var stamped: Bitmap? = null
        try {
            stamped = GeoStamp.render(decoded, StampText.lines(info))
            val bytes = ByteArrayOutputStream().use { stream ->
                val painted = stamped ?: return null
                if (!painted.compress(Bitmap.CompressFormat.JPEG, 92, stream)) return null
                stream.toByteArray()
            }
            return PhotoStore.save(context, bytes, info.capturedAtMillis, info)
        } finally {
            val painted = stamped
            if (painted != null && painted != decoded) decoded.recycle()
            (painted ?: decoded).recycle()
        }
    }

    private fun decode(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageScale.sampleSize(bounds.outWidth, bounds.outHeight, 2560)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }
}

package com.nt280793.geocamera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.max

object GeoStamp {
    fun render(source: Bitmap, lines: List<String>): Bitmap {
        if (source.width <= 0 || source.height <= 0) return source
        val bitmap = if (source.isMutable && source.config == Bitmap.Config.ARGB_8888) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }
        val canvas = Canvas(bitmap)
        val width = bitmap.width.toFloat()
        val pad = max(12f, width * 0.028f)
        val titleSize = max(22f, width * 0.032f)
        val bodySize = max(20f, width * 0.028f)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5C451")
            textSize = titleSize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(6f, 0f, 2f, Color.BLACK)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = bodySize
            typeface = Typeface.MONOSPACE
            setShadowLayer(6f, 0f, 2f, Color.BLACK)
        }
        val lineGap = bodySize * 1.38f
        val contentHeight = titleSize + lines.size * lineGap
        val boxHeight = pad * 1.6f + contentHeight
        val rect = RectF(
            pad,
            max(pad, bitmap.height - boxHeight - pad),
            bitmap.width - pad,
            bitmap.height - pad
        )
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(188, 8, 16, 14)
        }
        canvas.drawRoundRect(rect, pad, pad, background)
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5C451")
        }
        val accentWidth = max(6f, width * 0.008f)
        canvas.drawRoundRect(
            RectF(rect.left, rect.top, rect.left + accentWidth, rect.bottom),
            8f,
            8f,
            accent
        )
        val textLeft = rect.left + pad * 0.8f + accentWidth
        val maxText = rect.right - textLeft - pad * 0.4f
        var y = rect.top + pad * 0.85f + titleSize
        val brand = "NT"
        canvas.drawText(brand, textLeft, y, titlePaint)
        val brandWidth = titlePaint.measureText(brand)
        val tmPaint = Paint(titlePaint).apply {
            textSize = titleSize * 0.42f
            color = Color.parseColor("#F5C451")
        }
        canvas.drawText("TM", textLeft + brandWidth + 4f, y - titleSize * 0.42f, tmPaint)
        canvas.drawText("  GEO CAMERA", textLeft + brandWidth + tmPaint.measureText("TM") + 6f, y, titlePaint)
        for (line in lines) {
            y += lineGap
            canvas.drawText(fit(bodyPaint, line, maxText), textLeft, y, bodyPaint)
        }
        return bitmap
    }

    private fun fit(paint: Paint, text: String, maxWidth: Float): String {
        if (maxWidth <= 0f || paint.measureText(text) <= maxWidth) return text
        var trimmed = text
        while (trimmed.length > 1 && paint.measureText("$trimmed…") > maxWidth) {
            trimmed = trimmed.dropLast(1)
        }
        return "$trimmed…"
    }
}

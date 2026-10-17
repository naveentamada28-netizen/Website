package com.nt280793.geocamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object PhotoAlbumPdf {
    private const val margin = 28f
    private const val footerHeight = 40f

    fun write(context: Context, photos: List<Uri>): File? {
        if (photos.isEmpty()) return null
        val ordered = photos.asReversed()
        val document = PdfDocument()
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.nt_mark)
        try {
            val pages = ReportLayout.pageCount(ordered.size)
            for (pageIndex in 0 until pages) {
                val info = PdfDocument.PageInfo.Builder(
                    ReportLayout.pageWidth.toInt(),
                    ReportLayout.pageHeight.toInt(),
                    pageIndex + 1
                ).create()
                val page = document.startPage(info)
                val start = pageIndex * ReportLayout.perPage
                val slice = ordered.subList(start, minOf(start + ReportLayout.perPage, ordered.size))
                val heading = PageHeading.from(slice.map { PhotoCaption.readSite(context, it) })
                val contentTop = drawHeader(page.canvas, logo, heading)
                val cells = ReportLayout.cells(
                    margin = margin,
                    header = (contentTop - margin).coerceAtLeast(48f),
                    footer = footerHeight,
                    captionHeight = 52f
                )
                slice.forEachIndexed { index, uri ->
                    drawCell(context, page.canvas, cells[index], uri)
                }
                drawFooter(page.canvas, logo, pageIndex + 1, pages)
                document.finishPage(page)
            }
            val file = File(context.cacheDir, "GO-GEO-photos.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            return file
        } catch (_: Exception) {
            return null
        } finally {
            document.close()
            logo?.recycle()
        }
    }

    private fun drawHeader(canvas: Canvas, logo: Bitmap?, heading: PageHeading): Float {
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B2430")
            textSize = 20f
            isFakeBoldText = true
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B2430")
            textSize = 9f
        }
        val label = Paint(body).apply {
            color = Color.parseColor("#5C6B7A")
            textSize = 8f
            isFakeBoldText = true
        }
        logo?.let {
            canvas.drawBitmap(it, null, RectF(margin, 16f, margin + 32f, 48f), null)
        }
        canvas.drawText("GO-GEO", margin + 40f, 38f, title)
        var y = 62f
        y = drawLabeled(canvas, body, "Client", heading.clients, y)
        y = drawLabeled(canvas, body, "Mobile", heading.mobiles, y)
        y = drawLabeled(canvas, body, "Landmark", heading.landmarks, y)
        if (heading.hasSides()) y = drawBoundaryTable(canvas, heading, y)
        heading.boundaries.forEach { boundary ->
            y = drawWrapped(canvas, body, "Boundary: $boundary", y)
        }
        y = drawLabeled(canvas, body, "Note", heading.notes, y)
        if (y < 58f) y = 58f
        canvas.drawLine(margin, y, ReportLayout.pageWidth - margin, y, label)
        return y + 8f
    }

    private fun drawLabeled(canvas: Canvas, paint: Paint, label: String, values: List<String>, top: Float): Float {
        if (values.isEmpty()) return top
        return drawWrapped(canvas, paint, "$label: ${values.joinToString("  ·  ")}", top)
    }

    private fun drawWrapped(canvas: Canvas, paint: Paint, text: String, top: Float): Float {
        val width = ReportLayout.pageWidth - margin * 2f
        var y = top
        wrap(paint, text, width).forEach { line ->
            y += 12f
            canvas.drawText(line, margin, y, paint)
        }
        return y + 2f
    }

    private fun drawBoundaryTable(canvas: Canvas, heading: PageHeading, top: Float): Float {
        val values = listOf(
            "North" to heading.north.joinToString(" · "),
            "East" to heading.east.joinToString(" · "),
            "South" to heading.south.joinToString(" · "),
            "West" to heading.west.joinToString(" · ")
        )
        val colWidth = (ReportLayout.pageWidth - margin * 2f) / 2f
        val rowHeight = 34f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F4F7FB") }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D5E0EE")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5C6B7A")
            textSize = 8f
            isFakeBoldText = true
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B2430")
            textSize = 9f
        }
        values.forEachIndexed { index, (name, value) ->
            val column = index % 2
            val row = index / 2
            val left = margin + column * colWidth
            val cellTop = top + 4f + row * rowHeight
            val rect = RectF(left, cellTop, left + colWidth - 6f, cellTop + rowHeight - 4f)
            canvas.drawRoundRect(rect, 4f, 4f, fill)
            canvas.drawRoundRect(rect, 4f, 4f, stroke)
            canvas.drawText(name, rect.left + 6f, rect.top + 12f, label)
            canvas.drawText(fit(valuePaint, value.ifBlank { "—" }, rect.width() - 12f), rect.left + 6f, rect.top + 24f, valuePaint)
        }
        return top + 4f + rowHeight * 2f
    }

    private fun drawFooter(canvas: Canvas, logo: Bitmap?, page: Int, pages: Int) {
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D5E0EE"); strokeWidth = 1f }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B2430")
            textSize = 10f
            isFakeBoldText = true
        }
        val gold = Paint(text).apply { color = Color.parseColor("#A8842D"); textSize = 8f }
        val footerTop = ReportLayout.pageHeight - footerHeight
        canvas.drawLine(margin, footerTop, ReportLayout.pageWidth - margin, footerTop, line)
        logo?.let {
            canvas.drawBitmap(it, null, RectF(margin, footerTop + 8f, margin + 22f, footerTop + 30f), null)
        }
        canvas.drawText("NT", margin + 26f, footerTop + 23f, text)
        gold.textSize = 7f
        canvas.drawText("TM", margin + 26f + text.measureText("NT") + 1f, footerTop + 16f, gold)
        val pageLabel = "Page $page / $pages"
        canvas.drawText(pageLabel, ReportLayout.pageWidth - margin - text.measureText(pageLabel), footerTop + 23f, text)
    }

    private fun drawCell(context: Context, canvas: Canvas, cell: ReportCell, uri: Uri) {
        if (cell.bottom - cell.top < 8f) return
        val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D5E0EE")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val rect = RectF(cell.left, cell.top, cell.right, cell.bottom)
        canvas.drawRoundRect(rect, 8f, 8f, card)
        canvas.drawRoundRect(rect, 8f, 8f, border)
        val bitmap = decode(context, uri)
        val imageBottom = cell.imageBottom.coerceAtMost(cell.bottom - 8f).coerceAtLeast(cell.top + 8f)
        val image = RectF(cell.left + 6f, cell.top + 6f, cell.right - 6f, imageBottom)
        if (bitmap != null && image.height() > 4f) {
            canvas.drawBitmap(bitmap, null, image, null)
            bitmap.recycle()
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B2430")
            textSize = 8f
        }
        var y = imageBottom + 11f
        PhotoCaption.captureLines(context, uri).take(4).forEach { line ->
            if (y < cell.bottom - 2f) {
                canvas.drawText(fit(text, line, cell.right - cell.left - 12f), cell.left + 6f, y, text)
            }
            y += 10f
        }
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply {
                inSampleSize = ImageScale.sampleSize(bounds.outWidth, bounds.outHeight, 900)
            }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } catch (_: Exception) {
            null
        }
    }

    private fun wrap(paint: Paint, text: String, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines += current
                current = word
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines.ifEmpty { listOf(text) }
    }

    private fun fit(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var trimmed = text
        while (trimmed.length > 1 && paint.measureText("$trimmed…") > maxWidth) trimmed = trimmed.dropLast(1)
        return "$trimmed…"
    }
}

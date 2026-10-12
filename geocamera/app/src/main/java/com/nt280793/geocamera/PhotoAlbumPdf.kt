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
    fun write(context: Context, photos: List<Uri>): File? {
        if (photos.isEmpty()) return null
        val ordered = photos.asReversed()
        val document = PdfDocument()
        val cells = ReportLayout.cells()
        val pages = ReportLayout.pageCount(ordered.size)
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.nt_mark)
        try {
            for (pageIndex in 0 until pages) {
                val info = PdfDocument.PageInfo.Builder(
                    ReportLayout.pageWidth.toInt(),
                    ReportLayout.pageHeight.toInt(),
                    pageIndex + 1
                ).create()
                val page = document.startPage(info)
                drawHeader(page.canvas, logo, pageIndex + 1, pages)
                val start = pageIndex * ReportLayout.perPage
                val slice = ordered.subList(start, minOf(start + ReportLayout.perPage, ordered.size))
                slice.forEachIndexed { index, uri ->
                    drawCell(context, page.canvas, cells[index], uri)
                }
                document.finishPage(page)
            }
            val file = File(context.cacheDir, "NT-GeoCamera-photos.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            return file
        } catch (_: Exception) {
            return null
        } finally {
            document.close()
            logo?.recycle()
        }
    }

    private fun drawHeader(canvas: Canvas, logo: Bitmap?, page: Int, pages: Int) {
        logo?.let {
            canvas.drawBitmap(it, null, RectF(28f, 18f, 68f, 58f), null)
        }
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B2430")
            textSize = 16f
            isFakeBoldText = true
        }
        val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5C6B7A")
            textSize = 10f
        }
        canvas.drawText("NT", 76f, 36f, title)
        val tm = Paint(title).apply { textSize = 8f; color = Color.parseColor("#A8842D") }
        canvas.drawText("TM", 76f + title.measureText("NT") + 2f, 28f, tm)
        canvas.drawText("Geo Camera", 76f + title.measureText("NT") + 18f, 36f, title)
        canvas.drawText("Trademark of NT  ·  4 photos per page  ·  $page / $pages", 76f, 52f, sub)
    }

    private fun drawCell(context: Context, canvas: Canvas, cell: ReportCell, uri: Uri) {
        val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D5E0EE")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val rect = RectF(cell.left, cell.top, cell.right, cell.bottom)
        canvas.drawRoundRect(rect, 10f, 10f, card)
        canvas.drawRoundRect(rect, 10f, 10f, border)
        val bitmap = decode(context, uri)
        val image = RectF(cell.left + 8f, cell.top + 8f, cell.right - 8f, cell.imageBottom)
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, null, image, null)
            bitmap.recycle()
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B2430")
            textSize = 8.5f
        }
        var y = cell.imageBottom + 12f
        PhotoCaption.lines(context, uri).take(12).forEach { line ->
            canvas.drawText(fit(text, line, cell.right - cell.left - 16f), cell.left + 8f, y, text)
            y += 11f
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

    private fun fit(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var trimmed = text
        while (trimmed.length > 1 && paint.measureText("$trimmed…") > maxWidth) trimmed = trimmed.dropLast(1)
        return "$trimmed…"
    }
}

package com.nt280793.geocamera

data class ReportCell(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val imageBottom: Float
)

object ReportLayout {
    const val pageWidth = 595f
    const val pageHeight = 842f
    const val perPage = 4

    fun pageCount(itemCount: Int): Int {
        if (itemCount <= 0) return 0
        return (itemCount + perPage - 1) / perPage
    }

    fun cells(
        margin: Float = 28f,
        header: Float = 120f,
        footer: Float = 40f,
        gap: Float = 10f,
        captionHeight: Float = 52f
    ): List<ReportCell> {
        val topStart = margin + header
        val gridBottom = pageHeight - margin - footer
        val columnWidth = (pageWidth - margin * 2f - gap) / 2f
        val rowHeight = ((gridBottom - topStart - gap) / 2f).coerceAtLeast(0f)
        val cells = mutableListOf<ReportCell>()
        for (row in 0..1) {
            for (column in 0..1) {
                val left = margin + column * (columnWidth + gap)
                val top = topStart + row * (rowHeight + gap)
                val right = left + columnWidth
                val bottom = top + rowHeight
                cells += ReportCell(left, top, right, bottom, bottom - captionHeight)
            }
        }
        return cells
    }
}

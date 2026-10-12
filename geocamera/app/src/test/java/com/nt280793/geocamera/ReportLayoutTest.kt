package com.nt280793.geocamera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportLayoutTest {
    @Test
    fun putsFourPhotosOnAPage() {
        assertEquals(0, ReportLayout.pageCount(0))
        assertEquals(1, ReportLayout.pageCount(1))
        assertEquals(1, ReportLayout.pageCount(4))
        assertEquals(2, ReportLayout.pageCount(5))
        assertEquals(3, ReportLayout.pageCount(9))
    }

    @Test
    fun cellsStayInsideThePageWithoutOverlap() {
        val cells = ReportLayout.cells()
        assertEquals(4, cells.size)
        cells.forEach { cell ->
            assertTrue(cell.left >= 28f)
            assertTrue(cell.top >= 28f)
            assertTrue(cell.right <= ReportLayout.pageWidth - 28f)
            assertTrue(cell.bottom <= ReportLayout.pageHeight - 28f)
            assertTrue(cell.imageBottom < cell.bottom)
            assertTrue(cell.right > cell.left)
        }
        assertTrue(cells[0].right < cells[1].left)
        assertTrue(cells[0].bottom < cells[2].top)
    }
}

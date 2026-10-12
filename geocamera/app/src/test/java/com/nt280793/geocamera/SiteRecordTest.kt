package com.nt280793.geocamera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteRecordTest {
    @Test
    fun keepsClientMobileLandmarkAndBoundary() {
        val site = SiteRecord(
            client = "Ravi Kumar, owner",
            mobile = "+91 98765 43210",
            landmark = "Near the water tank",
            north = "Main road",
            east = "Plot 12",
            south = "Canal",
            west = "Lane",
            corners = listOf(
                BoundaryPoint(17.385044, 78.486671),
                BoundaryPoint(17.385200, 78.486900),
                BoundaryPoint(17.384900, 78.487100)
            )
        ).cleaned()
        val lines = site.lines(useDms = false)
        assertEquals("919876543210", site.mobile)
        assertTrue(lines.any { it == "Client: Ravi Kumar, owner" })
        assertTrue(lines.any { it == "Mobile: 919876543210" })
        assertTrue(lines.any { it == "Landmark: Near the water tank" })
        assertTrue(lines.any { it.contains("N Main road") && it.contains("E Plot 12") && it.contains("W Lane") })
        assertTrue(lines.any { it.startsWith("Boundary:") && it.contains("(closed)") })
        assertTrue(site.summary().contains("919876543210"))
        assertTrue(site.summary().contains("3 corners"))
    }

    @Test
    fun roundTripsThroughThePhotoDescription() {
        val original = SiteRecord(
            client = "Asha",
            mobile = "9876543210",
            landmark = "Temple corner",
            north = "Road",
            corners = listOf(BoundaryPoint(12.971599, 77.594566))
        )
        val encoded = original.encode("Plot 14")
        val decoded = SiteRecord.decode(encoded)
        assertTrue(decoded.recognized)
        assertEquals("Asha", decoded.site.client)
        assertEquals("9876543210", decoded.site.mobile)
        assertEquals("Temple corner", decoded.site.landmark)
        assertEquals("Road", decoded.site.north)
        assertEquals("Plot 14", decoded.note)
        assertEquals(12.971599, decoded.site.corners.single().latitude, 0.000001)
        assertEquals(77.594566, decoded.site.corners.single().longitude, 0.000001)
        assertFalse(decoded.site.lines(false).any { it.contains("(closed)") })
    }

    @Test
    fun leavesOlderNotesUntouched() {
        val decoded = SiteRecord.decode("Plot 14")
        assertFalse(decoded.recognized)
        assertTrue(decoded.site.isEmpty())
        assertEquals("", decoded.note)
    }

    @Test
    fun skipsAnEmptyRecord() {
        assertNull(SiteRecord().encode("   "))
        assertTrue(SiteRecord().lines(false).isEmpty())
    }

    @Test
    fun checksTheMobileNumber() {
        assertTrue(SiteRecord.mobileOk(""))
        assertTrue(SiteRecord.mobileOk("+91 98765 43210"))
        assertFalse(SiteRecord.mobileOk("12345"))
        assertFalse(SiteRecord.mobileOk("not a number"))
    }
}

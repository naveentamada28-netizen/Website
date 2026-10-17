package com.nt280793.geocamera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageHeadingTest {
    @Test
    fun putsCustomerDetailsAndEveryBoundaryInTheHeading() {
        val heading = PageHeading.from(
            listOf(
                DecodedSite(
                    SiteRecord(
                        client = "Ravi Kumar",
                        mobile = "9876543210",
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
                    ),
                    note = "Job 4",
                    recognized = true
                )
            )
        )
        assertEquals(listOf("Ravi Kumar"), heading.clients)
        assertEquals(listOf("9876543210"), heading.mobiles)
        assertEquals(listOf("Near the water tank"), heading.landmarks)
        assertEquals(listOf("Main road"), heading.north)
        assertEquals(listOf("Plot 12"), heading.east)
        assertEquals(listOf("Canal"), heading.south)
        assertEquals(listOf("Lane"), heading.west)
        assertEquals(listOf("Job 4"), heading.notes)
        assertTrue(heading.boundaries.single().contains("(closed)"))
        assertTrue(heading.hasSides())
    }

    @Test
    fun keepsOneCopyWhenPhotosShareTheSameCustomer() {
        val record = DecodedSite(
            SiteRecord(client = "Asha", mobile = "9876543210"),
            note = "",
            recognized = true
        )
        val heading = PageHeading.from(listOf(record, record))
        assertEquals(listOf("Asha"), heading.clients)
        assertEquals(listOf("9876543210"), heading.mobiles)
    }
}

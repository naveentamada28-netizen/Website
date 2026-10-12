package com.nt280793.geocamera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.util.Locale

class StampTextTest {
    @Test
    fun buildsTheFieldStamp() {
        assertEquals(
            listOf(
                "01 Jan 1970  00:00:00",
                "17.385044° N   78.486671° E",
                "Hyderabad, Telangana",
                "Acc ±8 m   Alt 542 m   HDG 124°",
                "Plot 14"
            ),
            StampText.lines(sample())
        )
    }

    @Test
    fun usesDmsWhenRequested() {
        val lines = StampText.lines(sample().copy(useDms = true))
        assertTrue(lines.any { it == "17° 23' 06.2\" N   78° 29' 12.0\" E" })
    }

    @Test
    fun hidesOptionalLines() {
        val lines = StampText.lines(
            sample().copy(showAddress = false, showAltitude = false, showHeading = false, note = "  ")
        )
        assertEquals(3, lines.size)
        assertFalse(lines.any { it.contains("Hyderabad") })
        assertFalse(lines.any { it.contains("Alt") })
        assertFalse(lines.any { it.contains("HDG") })
        assertTrue(lines.any { it.contains("Acc") })
    }

    @Test
    fun stampsTheClientLandmarkAndBoundary() {
        val lines = StampText.lines(
            sample().copy(
                site = SiteRecord(
                    client = "Ravi Kumar",
                    mobile = "9876543210",
                    landmark = "Near the water tank",
                    north = "Main road",
                    east = "Plot 12",
                    south = "Canal",
                    west = "Lane"
                )
            )
        )
        val text = lines.joinToString("\n")
        assertTrue(text.contains("Client: Ravi Kumar"))
        assertTrue(text.contains("Mobile: 9876543210"))
        assertTrue(text.contains("Landmark: Near the water tank"))
        assertTrue(text.contains("North: Main road") && text.contains("West: Lane"))
        assertEquals("Plot 14", lines.last())
    }

    @Test
    fun reportsMissingGps() {
        val lines = StampText.lines(
            sample().copy(
                latitude = null,
                longitude = null,
                accuracyM = null,
                altitudeM = null,
                headingDeg = null,
                address = null,
                note = null
            )
        )
        assertEquals(listOf("01 Jan 1970  00:00:00", "GPS unavailable"), lines)
    }

    private fun sample() = StampInfo(
        capturedAtMillis = 0L,
        latitude = 17.385044,
        longitude = 78.486671,
        accuracyM = 8.4f,
        altitudeM = 541.6,
        headingDeg = 123.6f,
        address = "Hyderabad, Telangana",
        note = "Plot 14",
        useDms = false,
        showAddress = true,
        showAltitude = true,
        showHeading = true,
        zone = ZoneOffset.UTC,
        locale = Locale.US
    )
}

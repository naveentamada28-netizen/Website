package com.nt280793.geocamera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset
import java.util.Locale

class GeoFormatTest {
    @Test
    fun decimalHemispheres() {
        assertEquals("17.385044° N", GeoFormat.latitude(17.385044))
        assertEquals("78.486671° E", GeoFormat.longitude(78.486671))
        assertEquals("33.868800° S", GeoFormat.latitude(-33.8688))
        assertEquals("151.209300° W", GeoFormat.longitude(-151.2093))
    }

    @Test
    fun dmsLabels() {
        assertEquals("17° 23' 06.2\" N", GeoFormat.latitudeDms(17.385044))
        assertEquals("78° 29' 12.0\" E", GeoFormat.longitudeDms(78.486671))
        assertEquals("33° 52' 07.7\" S", GeoFormat.latitudeDms(-33.8688))
    }

    @Test
    fun dmsCarriesSecondsIntoTheNextDegree() {
        val value = 10 + 59 / 60.0 + 59.96 / 3600.0
        assertEquals("11° 00' 00.0\" N", GeoFormat.latitudeDms(value))
    }

    @Test
    fun timestampsUseTheRequestedZone() {
        assertEquals(
            "01 Jan 1970  00:00:00",
            GeoFormat.formatWhen(0L, ZoneOffset.UTC, Locale.US)
        )
        assertEquals("1970:01:01 00:00:00", GeoFormat.exifDateTime(0L, ZoneOffset.UTC))
        assertEquals("19700101_000000_000", GeoFormat.fileStamp(0L, ZoneOffset.UTC))
    }

    @Test
    fun prettyExifDateAndRational() {
        assertEquals(
            "11 Oct 2026  14:05:06",
            GeoFormat.prettyExifDate("2026:10:11 14:05:06", Locale.US)
        )
        assertEquals(124.0, GeoFormat.rationalToDouble("124/1")!!, 0.001)
        assertEquals(12.5, GeoFormat.rationalToDouble("25/2")!!, 0.001)
        assertNull(GeoFormat.rationalToDouble("nope"))
        assertNull(GeoFormat.rationalToDouble("1/0"))
    }
}

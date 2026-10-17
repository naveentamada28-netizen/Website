package com.nt280793.geocamera

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageScaleTest {
    @Test
    fun keepsImagesInsideTheLongestEdge() {
        assertEquals(1, ImageScale.sampleSize(0, 0, 2560))
        assertEquals(1, ImageScale.sampleSize(2000, 1000, 2560))
        assertEquals(1, ImageScale.sampleSize(2560, 2560, 2560))
        assertEquals(2, ImageScale.sampleSize(2561, 100, 2560))
        assertEquals(4, ImageScale.sampleSize(8000, 6000, 2560))
    }
}

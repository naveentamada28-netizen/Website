package com.nt280793.geocamera

object ImageScale {
    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        if (width <= 0 || height <= 0 || maxEdge <= 0) return 1
        var sample = 1
        while (sample < 64 && (width / sample > maxEdge || height / sample > maxEdge)) {
            sample *= 2
        }
        return sample
    }
}

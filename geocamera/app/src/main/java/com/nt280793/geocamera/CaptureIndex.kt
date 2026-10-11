package com.nt280793.geocamera

import android.content.Context
import android.net.Uri

class CaptureIndex(context: Context) {
    private val prefs = context.getSharedPreferences("geo_index", Context.MODE_PRIVATE)

    fun add(uri: Uri) {
        val next = all().map { it.toString() }.toMutableList()
        next.removeAll { it == uri.toString() }
        next.add(0, uri.toString())
        prefs.edit().putString(KEY, next.take(300).joinToString("\n")).apply()
    }

    fun remove(uri: Uri) {
        val next = all().map { it.toString() }.filterNot { it == uri.toString() }
        prefs.edit().putString(KEY, next.joinToString("\n")).apply()
    }

    fun all(): List<Uri> {
        return prefs.getString(KEY, "").orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { Uri.parse(it) }
            .toList()
    }

    private companion object {
        const val KEY = "uris"
    }
}

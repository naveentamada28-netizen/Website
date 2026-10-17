package com.nt280793.geocamera

import java.util.Locale

data class BoundaryPoint(val latitude: Double, val longitude: Double)

data class SiteRecord(
    val client: String = "",
    val mobile: String = "",
    val landmark: String = "",
    val north: String = "",
    val east: String = "",
    val south: String = "",
    val west: String = "",
    val corners: List<BoundaryPoint> = emptyList()
) {
    fun cleaned(): SiteRecord = copy(
        client = squash(client, 160),
        mobile = digits(mobile).take(15),
        landmark = squash(landmark, 160),
        north = squash(north, 80),
        east = squash(east, 80),
        south = squash(south, 80),
        west = squash(west, 80),
        corners = corners.filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }.take(MAX_CORNERS)
    )

    fun isEmpty(): Boolean {
        val clean = cleaned()
        return clean.client.isEmpty() && clean.mobile.isEmpty() && clean.landmark.isEmpty() &&
            clean.north.isEmpty() && clean.east.isEmpty() && clean.south.isEmpty() &&
            clean.west.isEmpty() && clean.corners.isEmpty()
    }

    fun summary(): String {
        val clean = cleaned()
        val parts = mutableListOf<String>()
        if (clean.client.isNotEmpty()) parts += clean.client
        if (clean.mobile.isNotEmpty()) parts += clean.mobile
        if (clean.landmark.isNotEmpty()) parts += clean.landmark
        val hasSides = listOf(clean.north, clean.east, clean.south, clean.west).any { it.isNotEmpty() }
        if (clean.corners.isNotEmpty()) parts += "${clean.corners.size} corners"
        else if (hasSides) parts += "bounds"
        return parts.joinToString(" · ")
    }

    fun lines(useDms: Boolean): List<String> {
        val clean = cleaned()
        val out = mutableListOf<String>()
        labeled(out, "Client", clean.client)
        if (clean.mobile.isNotEmpty()) out.addAll(wrap("Mobile: ${clean.mobile}"))
        labeled(out, "Landmark", clean.landmark)
        labeled(out, "North", clean.north)
        labeled(out, "East", clean.east)
        labeled(out, "South", clean.south)
        labeled(out, "West", clean.west)
        if (clean.corners.isNotEmpty()) {
            val path = clean.corners.joinToString(" → ") { pointText(it, useDms) }
            val closed = if (clean.corners.size >= 3) " (closed)" else ""
            out.addAll(wrap("Boundary: $path$closed"))
        }
        return out
    }

    fun cornerList(useDms: Boolean): String {
        val clean = cleaned()
        if (clean.corners.isEmpty()) return ""
        return clean.corners.mapIndexed { index, point ->
            "${index + 1}. ${pointText(point, useDms)}"
        }.joinToString("\n")
    }

    fun encode(note: String = ""): String? {
        val clean = cleaned()
        val noteLine = squash(note, 80)
        if (clean.isEmpty() && noteLine.isEmpty()) return null
        val rows = mutableListOf(HEADER)
        fun put(key: String, value: String) {
            if (value.isNotEmpty()) rows += "$key=$value"
        }
        put("client", clean.client)
        put("mobile", clean.mobile)
        put("landmark", clean.landmark)
        put("north", clean.north)
        put("east", clean.east)
        put("south", clean.south)
        put("west", clean.west)
        clean.corners.forEach { point ->
            rows += String.format(Locale.US, "corner=%.6f,%.6f", point.latitude, point.longitude)
        }
        put("note", noteLine)
        return rows.joinToString("\n")
    }

    private fun labeled(out: MutableList<String>, label: String, value: String) {
        if (value.isNotEmpty()) out.addAll(wrap("$label: $value"))
    }

    companion object {
        const val HEADER = "NTSITE/1"
        const val MAX_CORNERS = 20
        private const val WRAP = 46

        fun digits(raw: String): String = raw.filter { it.isDigit() }

        fun mobileOk(raw: String): Boolean {
            if (raw.isBlank()) return true
            return digits(raw).length in 10..15
        }

        fun decode(raw: String?): DecodedSite {
            val text = raw?.trim().orEmpty()
            if (!text.startsWith(HEADER)) return DecodedSite(SiteRecord(), "", false)
            var client = ""
            var mobile = ""
            var landmark = ""
            var north = ""
            var east = ""
            var south = ""
            var west = ""
            var note = ""
            val corners = mutableListOf<BoundaryPoint>()
            text.lineSequence().drop(1).forEach { line ->
                val split = line.indexOf('=')
                if (split <= 0) return@forEach
                val key = line.substring(0, split)
                val value = line.substring(split + 1).trim()
                when (key) {
                    "client" -> client = value
                    "mobile" -> mobile = value
                    "landmark" -> landmark = value
                    "north" -> north = value
                    "east" -> east = value
                    "south" -> south = value
                    "west" -> west = value
                    "note" -> note = value
                    "corner" -> parseCorner(value)?.let { corners += it }
                }
            }
            return DecodedSite(
                SiteRecord(client, mobile, landmark, north, east, south, west, corners).cleaned(),
                note,
                true
            )
        }

        private fun parseCorner(value: String): BoundaryPoint? {
            val parts = value.split(",")
            val latitude = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return null
            val longitude = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return null
            if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
            return BoundaryPoint(latitude, longitude)
        }

        private fun squash(raw: String, limit: Int): String {
            return raw.replace('\n', ' ').replace('\r', ' ').trim().replace(Regex("\\s+"), " ").take(limit)
        }

        private fun pointText(point: BoundaryPoint, useDms: Boolean): String {
            val latitude = if (useDms) GeoFormat.latitudeDms(point.latitude) else GeoFormat.latitude(point.latitude)
            val longitude = if (useDms) GeoFormat.longitudeDms(point.longitude) else GeoFormat.longitude(point.longitude)
            return "$latitude $longitude"
        }

        private fun wrap(text: String): List<String> {
            val clean = text.trim()
            if (clean.length <= WRAP) return listOf(clean)
            val out = mutableListOf<String>()
            var rest = clean
            while (rest.length > WRAP) {
                val space = rest.lastIndexOf(' ', WRAP)
                val cut = if (space > 12) space else WRAP
                out += rest.substring(0, cut).trimEnd()
                rest = rest.substring(cut).trimStart()
            }
            if (rest.isNotEmpty()) out += rest
            return out
        }
    }
}

data class DecodedSite(
    val site: SiteRecord,
    val note: String,
    val recognized: Boolean
)

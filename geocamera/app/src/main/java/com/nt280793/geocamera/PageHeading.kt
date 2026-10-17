package com.nt280793.geocamera

data class PageHeading(
    val clients: List<String> = emptyList(),
    val mobiles: List<String> = emptyList(),
    val landmarks: List<String> = emptyList(),
    val north: List<String> = emptyList(),
    val east: List<String> = emptyList(),
    val south: List<String> = emptyList(),
    val west: List<String> = emptyList(),
    val boundaries: List<String> = emptyList(),
    val notes: List<String> = emptyList()
) {
    fun hasSides(): Boolean = listOf(north, east, south, west).any { it.isNotEmpty() }

    companion object {
        fun from(records: List<DecodedSite>): PageHeading {
            return PageHeading(
                clients = unique(records.map { it.site.client }),
                mobiles = unique(records.map { it.site.mobile }),
                landmarks = unique(records.map { it.site.landmark }),
                north = unique(records.map { it.site.north }),
                east = unique(records.map { it.site.east }),
                south = unique(records.map { it.site.south }),
                west = unique(records.map { it.site.west }),
                boundaries = unique(records.map { boundaryText(it.site) }),
                notes = unique(records.map { it.note })
            )
        }

        fun boundaryText(site: SiteRecord): String {
            val corners = site.cleaned().corners
            if (corners.isEmpty()) return ""
            val path = corners.joinToString(" → ") { point ->
                "${GeoFormat.latitude(point.latitude)} ${GeoFormat.longitude(point.longitude)}"
            }
            return if (corners.size >= 3) "$path (closed)" else path
        }

        private fun unique(values: List<String>): List<String> {
            return values.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        }
    }
}

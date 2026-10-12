package com.nt280793.geocamera

import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

data class StampInfo(
    val capturedAtMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyM: Float?,
    val altitudeM: Double?,
    val headingDeg: Float?,
    val address: String?,
    val note: String?,
    val site: SiteRecord = SiteRecord(),
    val useDms: Boolean,
    val showAddress: Boolean,
    val showAltitude: Boolean,
    val showHeading: Boolean,
    val zone: ZoneId = ZoneId.systemDefault(),
    val locale: Locale = Locale.getDefault()
)

object StampText {
    fun lines(info: StampInfo): List<String> {
        val out = mutableListOf<String>()
        out += GeoFormat.formatWhen(info.capturedAtMillis, info.zone, info.locale)
        val lat = info.latitude
        val lon = info.longitude
        out += if (lat != null && lon != null) {
            if (info.useDms) {
                "${GeoFormat.latitudeDms(lat)}   ${GeoFormat.longitudeDms(lon)}"
            } else {
                "${GeoFormat.latitude(lat)}   ${GeoFormat.longitude(lon)}"
            }
        } else {
            "GPS unavailable"
        }
        if (info.showAddress) {
            info.address?.trim()?.takeIf { it.isNotEmpty() }?.let { out += it }
        }
        val meta = mutableListOf<String>()
        info.accuracyM?.let { meta += "Acc ±${it.roundToInt()} m" }
        if (info.showAltitude) info.altitudeM?.let { meta += "Alt ${it.roundToInt()} m" }
        if (info.showHeading) info.headingDeg?.let { meta += headingText(it) }
        if (meta.isNotEmpty()) out += meta.joinToString("   ")
        out += info.site.lines(info.useDms)
        info.note
            ?.replace('\n', ' ')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { out += it }
        return out
    }

    fun block(info: StampInfo): String = lines(info).joinToString("\n")

    private fun headingText(degrees: Float): String {
        var heading = degrees.roundToInt() % 360
        if (heading < 0) heading += 360
        return "HDG $heading°"
    }
}

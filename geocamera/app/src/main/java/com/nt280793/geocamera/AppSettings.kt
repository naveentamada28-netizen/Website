package com.nt280793.geocamera

import android.content.Context

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("geo_camera", Context.MODE_PRIVATE)

    var useDms: Boolean
        get() = prefs.getBoolean(KEY_DMS, false)
        set(value) = prefs.edit().putBoolean(KEY_DMS, value).apply()

    var showAddress: Boolean
        get() = prefs.getBoolean(KEY_ADDRESS, true)
        set(value) = prefs.edit().putBoolean(KEY_ADDRESS, value).apply()

    var showAltitude: Boolean
        get() = prefs.getBoolean(KEY_ALTITUDE, true)
        set(value) = prefs.edit().putBoolean(KEY_ALTITUDE, value).apply()

    var showHeading: Boolean
        get() = prefs.getBoolean(KEY_HEADING, true)
        set(value) = prefs.edit().putBoolean(KEY_HEADING, value).apply()

    var note: String
        get() = prefs.getString(KEY_NOTE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_NOTE, value).apply()

    var site: SiteRecord
        get() = SiteRecord.decode(prefs.getString(KEY_SITE, null)).site
        set(value) = prefs.edit().putString(KEY_SITE, value.cleaned().encode().orEmpty()).apply()

    var flash: String
        get() = prefs.getString(KEY_FLASH, "off") ?: "off"
        set(value) = prefs.edit().putString(KEY_FLASH, value).apply()

    private companion object {
        const val KEY_DMS = "dms"
        const val KEY_ADDRESS = "address"
        const val KEY_ALTITUDE = "altitude"
        const val KEY_HEADING = "heading"
        const val KEY_NOTE = "note"
        const val KEY_SITE = "site"
        const val KEY_FLASH = "flash"
    }
}

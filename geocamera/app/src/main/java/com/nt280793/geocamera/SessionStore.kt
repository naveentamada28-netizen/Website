package com.nt280793.geocamera

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("nt_session", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_SERVER, value.trim().trimEnd('/')).apply()

    var token: String
        get() = prefs.getString(KEY_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var mobile: String
        get() = prefs.getString(KEY_MOBILE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_MOBILE, value).apply()

    fun isLoggedIn(): Boolean = token.isNotBlank() && serverUrl.isNotBlank()

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_MOBILE).apply()
    }

    private companion object {
        const val KEY_SERVER = "server"
        const val KEY_TOKEN = "token"
        const val KEY_MOBILE = "mobile"
    }
}

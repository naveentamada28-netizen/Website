package com.nt280793.geocamera

object AccountForm {
    fun normalizeMobile(raw: String): String = raw.filter { it.isDigit() }

    fun mobileOk(raw: String): Boolean = normalizeMobile(raw).length in 10..15

    fun passwordOk(raw: String): Boolean = raw.length >= 6

    fun serverOk(raw: String): Boolean {
        val value = raw.trim().trimEnd('/')
        return value.startsWith("https://") || value.startsWith("http://")
    }
}

package com.nt280793.geocamera

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AuthApi {
    data class Response(val ok: Boolean, val message: String, val token: String? = null, val mobile: String? = null)

    fun register(server: String, mobile: String, password: String): Response =
        post(server, "register.php", mobile, password)

    fun login(server: String, mobile: String, password: String): Response =
        post(server, "login.php", mobile, password)

    private fun post(server: String, path: String, mobile: String, password: String): Response {
        val endpoint = server.trim().trimEnd('/') + "/api/" + path
        val payload = JSONObject()
            .put("mobile", AccountForm.normalizeMobile(mobile))
            .put("password", password)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (text.startsWith("{")) JSONObject(text) else JSONObject()
            Response(
                ok = json.optBoolean("ok") && code in 200..299,
                message = json.optString("message").ifBlank { "Could not reach the hosting website." },
                token = json.optString("token").ifBlank { null },
                mobile = json.optString("mobile").ifBlank { null }
            )
        } catch (_: Exception) {
            Response(false, "Could not reach the hosting website.")
        } finally {
            connection.disconnect()
        }
    }
}

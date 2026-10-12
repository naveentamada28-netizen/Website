package com.nt280793.geocamera

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.nt280793.geocamera.databinding.ActivityLoginBinding
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionStore
    private val io = Executors.newSingleThreadExecutor()
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionStore(this)
        if (session.isLoggedIn()) {
            openCamera()
            return
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.serverInput.setText(session.serverUrl)
        binding.loginButton.setOnClickListener { submit(register = false) }
        binding.registerButton.setOnClickListener { submit(register = true) }
    }

    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }

    private fun submit(register: Boolean) {
        if (busy || !::binding.isInitialized) return
        val server = binding.serverInput.text?.toString().orEmpty()
        val mobile = binding.mobileInput.text?.toString().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        when {
            !AccountForm.serverOk(server) -> binding.statusText.setText(R.string.bad_server)
            !AccountForm.mobileOk(mobile) -> binding.statusText.setText(R.string.bad_mobile)
            !AccountForm.passwordOk(password) -> binding.statusText.setText(R.string.bad_password)
            else -> {
                busy = true
                setBusy(true)
                session.serverUrl = server
                io.execute {
                    val response = if (register) {
                        AuthApi.register(session.serverUrl, mobile, password)
                    } else {
                        AuthApi.login(session.serverUrl, mobile, password)
                    }
                    val signedIn = if (register && response.ok) {
                        AuthApi.login(session.serverUrl, mobile, password)
                    } else {
                        response
                    }
                    runOnUiThread {
                        busy = false
                        if (isDestroyed) return@runOnUiThread
                        setBusy(false)
                        if (signedIn.ok && !signedIn.token.isNullOrBlank()) {
                            session.token = signedIn.token
                            session.mobile = signedIn.mobile ?: AccountForm.normalizeMobile(mobile)
                            openCamera()
                        } else {
                            binding.statusText.text = signedIn.message
                        }
                    }
                }
            }
        }
    }

    private fun setBusy(value: Boolean) {
        binding.loginButton.isEnabled = !value
        binding.registerButton.isEnabled = !value
        binding.statusText.setText(if (value) R.string.contacting_host else R.string.login_help)
        binding.statusText.visibility = View.VISIBLE
    }

    private fun openCamera() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

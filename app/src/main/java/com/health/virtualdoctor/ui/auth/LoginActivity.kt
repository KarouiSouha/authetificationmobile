package com.health.virtualdoctor.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.health.virtualdoctor.R
import com.health.virtualdoctor.data.api.RetrofitClient
import com.health.virtualdoctor.data.models.LoginRequest
import com.health.virtualdoctor.utils.TokenManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvRegisterLink: android.view.View

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        window.statusBarColor = resources.getColor(R.color.transparent, theme)

        tokenManager = TokenManager(this)

        initViews()
        setupListeners()
        setupValidation()
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }

        tvRegisterLink.setOnClickListener {
            navigateToRegister()
        }
    }

    private fun setupValidation() {
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilEmail.error = null
            }
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilPassword.error = null
            }
        })
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            tilEmail.error = "L'email est requis"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Email invalide"
            isValid = false
        } else {
            tilEmail.error = null
        }

        val password = etPassword.text.toString()
        if (password.isEmpty()) {
            tilPassword.error = "Le mot de passe est requis"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Au moins 6 caractères"
            isValid = false
        } else {
            tilPassword.error = null
        }

        return isValid
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        btnLogin.isEnabled = false
        btnLogin.text = "Connexion..."

        lifecycleScope.launch {
            try {
                val request = LoginRequest(email, password)
                val response = RetrofitClient.getApiService(this@LoginActivity).login(request)

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!

                    Log.d("LoginActivity", "✅ Response: $authResponse")

                    // ✅ Sauvegarder les tokens
                    tokenManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)

                    // ✅ FIX: Gérer userId nullable (utiliser email si null)
                    val userId = authResponse.userId ?: authResponse.user.email
                    val role = authResponse.user.roles.firstOrNull() ?: "USER"

                    tokenManager.saveUserInfo(
                        userId = userId,
                        email = authResponse.user.email,
                        name = authResponse.user.fullName,
                        role = role
                    )

                    Toast.makeText(this@LoginActivity, "✅ Connexion réussie!", Toast.LENGTH_SHORT).show()

                    // ✅ Rediriger selon le rôle
                    navigateByRole(role)

                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Log.e("LoginActivity", "❌ Error: $errorBody")
                    Toast.makeText(this@LoginActivity, "❌ $errorBody", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("LoginActivity", "❌ Exception: ${e.message}", e)
                Toast.makeText(this@LoginActivity, "❌ Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                btnLogin.isEnabled = true
                btnLogin.text = getString(R.string.login_button)
            }
        }
    }

    private fun navigateByRole(role: String) {
        when (role) {
            "USER" -> {
                Toast.makeText(this, "🏠 User Home", Toast.LENGTH_SHORT).show()
            }
            "DOCTOR" -> {
                Toast.makeText(this, "👨‍⚕️ Doctor Dashboard", Toast.LENGTH_SHORT).show()
            }
            "ADMIN" -> {
                Toast.makeText(this, "⚙️ Admin Dashboard", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
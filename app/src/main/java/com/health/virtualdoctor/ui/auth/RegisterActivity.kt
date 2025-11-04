package com.health.virtualdoctor.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.health.virtualdoctor.R
import com.health.virtualdoctor.data.api.RetrofitClient
import com.health.virtualdoctor.data.models.RegisterRequest
import com.health.virtualdoctor.utils.TokenManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    // Basic Fields
    private lateinit var tilFirstName: TextInputLayout
    private lateinit var etFirstName: TextInputEditText
    private lateinit var tilLastName: TextInputLayout
    private lateinit var etLastName: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPhone: TextInputLayout
    private lateinit var etPhone: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText

    // User Type
    private lateinit var rgUserType: RadioGroup
    private lateinit var rbPatient: RadioButton
    private lateinit var rbDoctor: RadioButton

    // Doctor-Specific Fields Container
    private lateinit var llDoctorFields: LinearLayout

    // Doctor-Specific Fields
    private lateinit var tilSpecialization: TextInputLayout
    private lateinit var etSpecialization: TextInputEditText
    private lateinit var tilLicenseNumber: TextInputLayout
    private lateinit var etLicenseNumber: TextInputEditText
    private lateinit var tilYearsOfExperience: TextInputLayout
    private lateinit var etYearsOfExperience: TextInputEditText
    private lateinit var tilEducation: TextInputLayout
    private lateinit var etEducation: TextInputEditText
    private lateinit var tilClinicName: TextInputLayout
    private lateinit var etClinicName: TextInputEditText
    private lateinit var tilClinicAddress: TextInputLayout
    private lateinit var etClinicAddress: TextInputEditText

    private lateinit var cbTerms: CheckBox
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvLoginLink: TextView

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        window.statusBarColor = resources.getColor(R.color.transparent, theme)

        tokenManager = TokenManager(this)

        initViews()
        setupListeners()
        setupValidation()
    }

    private fun initViews() {
        // Basic fields
        tilFirstName = findViewById(R.id.tilFirstName)
        etFirstName = findViewById(R.id.etFirstName)
        tilLastName = findViewById(R.id.tilLastName)
        etLastName = findViewById(R.id.etLastName)
        tilEmail = findViewById(R.id.tilEmailRegister)
        etEmail = findViewById(R.id.etEmailRegister)
        tilPhone = findViewById(R.id.tilPhone)
        etPhone = findViewById(R.id.etPhone)
        tilPassword = findViewById(R.id.tilPasswordRegister)
        etPassword = findViewById(R.id.etPasswordRegister)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        // User type
        rgUserType = findViewById(R.id.rgUserType)
        rbPatient = findViewById(R.id.rbPatient)
        rbDoctor = findViewById(R.id.rbDoctor)

        // Doctor fields container
        llDoctorFields = findViewById(R.id.llDoctorFields)

        // Doctor-specific fields
        tilSpecialization = findViewById(R.id.tilSpecialization)
        etSpecialization = findViewById(R.id.etSpecialization)
        tilLicenseNumber = findViewById(R.id.tilLicenseNumber)
        etLicenseNumber = findViewById(R.id.etLicenseNumber)
        tilYearsOfExperience = findViewById(R.id.tilYearsOfExperience)
        etYearsOfExperience = findViewById(R.id.etYearsOfExperience)
        tilEducation = findViewById(R.id.tilEducation)
        etEducation = findViewById(R.id.etEducation)
        tilClinicName = findViewById(R.id.tilClinicName)
        etClinicName = findViewById(R.id.etClinicName)
        tilClinicAddress = findViewById(R.id.tilClinicAddress)
        etClinicAddress = findViewById(R.id.etClinicAddress)

        cbTerms = findViewById(R.id.cbTerms)
        btnRegister = findViewById(R.id.btnRegister)
        tvLoginLink = findViewById(R.id.tvLoginLink)
    }

    private fun setupListeners() {
        // User type selection
        rgUserType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbPatient -> showDoctorFields(false)
                R.id.rbDoctor -> showDoctorFields(true)
            }
        }

        btnRegister.setOnClickListener {
            if (validateInputs()) {
                performRegister()
            }
        }

        tvLoginLink.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun showDoctorFields(show: Boolean) {
        llDoctorFields.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setupValidation() {
        // Basic fields
        etFirstName.addTextChangedListener(createTextWatcher(tilFirstName))
        etLastName.addTextChangedListener(createTextWatcher(tilLastName))
        etEmail.addTextChangedListener(createTextWatcher(tilEmail))
        etPhone.addTextChangedListener(createTextWatcher(tilPhone))
        etPassword.addTextChangedListener(createTextWatcher(tilPassword))
        etConfirmPassword.addTextChangedListener(createTextWatcher(tilConfirmPassword))

        // Doctor fields
        etSpecialization.addTextChangedListener(createTextWatcher(tilSpecialization))
        etLicenseNumber.addTextChangedListener(createTextWatcher(tilLicenseNumber))
        etYearsOfExperience.addTextChangedListener(createTextWatcher(tilYearsOfExperience))
        etEducation.addTextChangedListener(createTextWatcher(tilEducation))
        etClinicName.addTextChangedListener(createTextWatcher(tilClinicName))
        etClinicAddress.addTextChangedListener(createTextWatcher(tilClinicAddress))
    }

    private fun createTextWatcher(textInputLayout: TextInputLayout): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                textInputLayout.error = null
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Basic validation
        val firstName = etFirstName.text.toString().trim()
        if (firstName.isEmpty()) {
            tilFirstName.error = "Le prénom est requis"
            isValid = false
        }

        val lastName = etLastName.text.toString().trim()
        if (lastName.isEmpty()) {
            tilLastName.error = "Le nom est requis"
            isValid = false
        }

        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            tilEmail.error = "L'email est requis"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Email invalide"
            isValid = false
        }

        val phone = etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            tilPhone.error = "Le téléphone est requis"
            isValid = false
        } else if (phone.length < 8) {
            tilPhone.error = "Numéro invalide"
            isValid = false
        }

        val password = etPassword.text.toString()
        if (password.isEmpty()) {
            tilPassword.error = "Le mot de passe est requis"
            isValid = false
        } else if (password.length < 8) {
            tilPassword.error = "Au moins 8 caractères"
            isValid = false
        }

        val confirmPassword = etConfirmPassword.text.toString()
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Confirmation requise"
            isValid = false
        } else if (password != confirmPassword) {
            tilConfirmPassword.error = "Les mots de passe ne correspondent pas"
            isValid = false
        }

        // Doctor-specific validation
        if (rbDoctor.isChecked) {
            val specialization = etSpecialization.text.toString().trim()
            if (specialization.isEmpty()) {
                tilSpecialization.error = "La spécialisation est requise"
                isValid = false
            }

            val licenseNumber = etLicenseNumber.text.toString().trim()
            if (licenseNumber.isEmpty()) {
                tilLicenseNumber.error = "Le numéro de licence est requis"
                isValid = false
            }

            val yearsOfExperience = etYearsOfExperience.text.toString().trim()
            if (yearsOfExperience.isEmpty()) {
                tilYearsOfExperience.error = "L'expérience est requise"
                isValid = false
            }

            val education = etEducation.text.toString().trim()
            if (education.isEmpty()) {
                tilEducation.error = "La formation est requise"
                isValid = false
            }

            val clinicName = etClinicName.text.toString().trim()
            if (clinicName.isEmpty()) {
                tilClinicName.error = "Le nom de la clinique est requis"
                isValid = false
            }

            val clinicAddress = etClinicAddress.text.toString().trim()
            if (clinicAddress.isEmpty()) {
                tilClinicAddress.error = "L'adresse est requise"
                isValid = false
            }
        }

        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Veuillez accepter les conditions", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun performRegister() {
        btnRegister.isEnabled = false
        btnRegister.text = "Inscription..."

        lifecycleScope.launch {
            try {
                val role = if (rbDoctor.isChecked) "DOCTOR" else "USER"

                val request = RegisterRequest(
                    email = etEmail.text.toString().trim(),
                    password = etPassword.text.toString(),
                    firstName = etFirstName.text.toString().trim(),
                    lastName = etLastName.text.toString().trim(),
                    birthDate = null, // TODO: Add date picker if needed
                    gender = null, // TODO: Add gender selection if needed
                    phoneNumber = etPhone.text.toString().trim(),
                    role = role,

                    // Doctor-specific fields
                    medicalLicenseNumber = if (rbDoctor.isChecked) etLicenseNumber.text.toString().trim() else null,
                    specialization = if (rbDoctor.isChecked) etSpecialization.text.toString().trim() else null,
                    hospitalAffiliation = if (rbDoctor.isChecked) etClinicName.text.toString().trim() else null,
                    yearsOfExperience = if (rbDoctor.isChecked) etYearsOfExperience.text.toString().trim().toIntOrNull() else null
                )

                // ✅ FIX: Use getApiService() instead of apiService
                val response = RetrofitClient.getApiService(this@RegisterActivity).register(request)

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!

                    // Sauvegarder les tokens
                    tokenManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)

                    // Sauvegarder les infos utilisateur
                    val userRole = authResponse.user.roles.firstOrNull() ?: "USER"
                    tokenManager.saveUserInfo(
                        authResponse.userId,
                        authResponse.user.email,
                        authResponse.user.fullName,
                        userRole
                    )

                    Toast.makeText(this@RegisterActivity, "✅ Inscription réussie!", Toast.LENGTH_SHORT).show()

                    // Rediriger selon le rôle
                    if (userRole == "DOCTOR" && !authResponse.user.isActivated) {
                        // Doctor en attente d'activation
                        Toast.makeText(
                            this@RegisterActivity,
                            "⏳ Votre compte est en attente d'activation par un administrateur",
                            Toast.LENGTH_LONG
                        ).show()
                        navigateToLogin()
                    } else {
                        navigateByRole(userRole)
                    }

                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@RegisterActivity, "❌ $errorBody", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "❌ Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            } finally {
                btnRegister.isEnabled = true
                btnRegister.text = getString(R.string.register_button)
            }
        }
    }

    private fun navigateByRole(role: String) {
        when (role) {
            "USER" -> {
                // TODO: Navigate to User Home
                Toast.makeText(this, "🏠 Bienvenue User!", Toast.LENGTH_SHORT).show()
            }
            "DOCTOR" -> {
                // TODO: Navigate to Doctor Dashboard
                Toast.makeText(this, "👨‍⚕️ Bienvenue Docteur!", Toast.LENGTH_SHORT).show()
            }
            "ADMIN" -> {
                // TODO: Navigate to Admin Dashboard
                Toast.makeText(this, "⚙️ Bienvenue Admin!", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
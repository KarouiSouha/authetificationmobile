package com.health.virtualdoctor.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.health.virtualdoctor.R
import com.health.virtualdoctor.data.api.RetrofitClient
import com.health.virtualdoctor.ui.welcome.WelcomeActivity
import com.health.virtualdoctor.utils.RoleManager
import com.health.virtualdoctor.utils.TokenManager
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private val splashTimeOut: Long = 2000
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        window.statusBarColor = resources.getColor(R.color.transparent, theme)
        
        tokenManager = TokenManager(this)

        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
        }, splashTimeOut)
    }

    private fun checkAuthAndNavigate() {
        if (tokenManager.isLoggedIn()) {
            // User has token, verify it's still valid
            verifyTokenAndNavigate()
        } else {
            // No token, go to welcome
            navigateToWelcome()
        }
    }

    private fun verifyTokenAndNavigate() {
        lifecycleScope.launch {
            try {
                val accessToken = tokenManager.getAccessToken()
                val response = RetrofitClient.getApiService(this@SplashActivity)
                    .getUserProfile("Bearer $accessToken")

                if (response.isSuccessful && response.body() != null) {
                    // Token valid, navigate by role
                    val role = tokenManager.getUserRole() ?: "USER"
                    RoleManager.navigateByRole(this@SplashActivity, role)
                } else {
                    // Token invalid, clear and go to welcome
                    tokenManager.clearTokens()
                    navigateToWelcome()
                }
            } catch (e: Exception) {
                // Error checking token, go to welcome
                e.printStackTrace()
                navigateToWelcome()
            }
        }
    }

    private fun navigateToWelcome() {
        val intent = Intent(this, WelcomeActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
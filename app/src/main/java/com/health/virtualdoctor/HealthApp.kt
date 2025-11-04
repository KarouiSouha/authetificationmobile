package com.health.virtualdoctor

import android.app.Application
import com.health.virtualdoctor.data.api.RetrofitClient

class HealthApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Retrofit with context
        RetrofitClient.init(this)
    }
}
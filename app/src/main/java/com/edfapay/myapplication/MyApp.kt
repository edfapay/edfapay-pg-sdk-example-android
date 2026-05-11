package com.edfapay.myapplication

import android.app.Application
import com.edfapay.payment_gateway.app.core.EdfaPgSdk

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        EdfaPgSdk.enableLogs = true
        EdfaPgSdk.init(
            apiKey = "08D0219992A52830E4A216841530C06344C81C72782B4CF8E3E4B5C8BBA69D0B",
            baseUrl = "https://dev-api.edfapay.com", // trailing slash optional; use env-specific URL
        )
    }
}
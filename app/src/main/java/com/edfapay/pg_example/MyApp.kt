package com.edfapay.pg_example

import android.app.Application
import com.edfapay.payment_gateway.app.core.EdfaPgSdk

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        EdfaPgSdk.enableLogs = true
        EdfaPgSdk.init(
            apiKey = "Add here Token here",
            baseUrl = "Base URL for ENV",
        )
    }
}
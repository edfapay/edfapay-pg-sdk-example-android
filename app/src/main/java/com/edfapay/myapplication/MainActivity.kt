package com.edfapay.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edfapay.myapplication.ui.theme.MyApplicationTheme
import com.edfapay.payment_gateway.app.core.EdfaPgSdk
import com.edfapay.payment_gateway.app.toolbox.EdfaPayDesignType
import com.edfapay.payment_gateway.app.toolbox.EdfaPayLanguage
import com.edfapay.payment_gateway.data.model.request.order.EdfaPgSaleOrder
import com.edfapay.payment_gateway.data.model.request.payer.EdfaPgPayer
import com.edfapay.payment_gateway.data.model.request.payer.EdfaPgPayerOptions
import com.edfapay.payment_gateway.presentation.EdfaPayUi
import com.edfapay.payment_gateway.presentation.EdfaWebView
import com.edfapay.payment_gateway.presentation.payment.models.EdfaCardPay
import java.util.UUID

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        EdfaPgSdk.enableLogs = true

        val order = EdfaPgSaleOrder(
            id = UUID.randomUUID().toString(),
            amount = 1.00,
            currency = "SAR",
            description = "Test order",
        )
        val payer = EdfaPgPayer(
            firstName = "First",
            lastName = "Last",
            address = "Street 1",
            country = "SA",
            city = "Riyadh",
            zip = "12345",
            email = "customer@example.com",
            phone = "+966500000000",
            ip = "203.0.113.10",
            // birthdate omitted (defaults to null). If you need it, add kotlinx-datetime and use kotlinx.datetime.LocalDate.
            options = EdfaPgPayerOptions(
                middleName = "Middle",
                address2 = "District",
                state = "Riyadh",
            ),
        )

        EdfaCardPay.shared()
            .setOrder(order)
            .setPayer(payer)
            .setDesignType(EdfaPayDesignType.ONE)
            .setLanguage(EdfaPayLanguage.en)
            .setDebug(true)
            .setResultUrls(
                successUrl = "https://edfapay.com/process-completed",
                failureUrl = "https://edfapay.com/process-failed",
            )
            .onTransactionSuccess { response ->
                Log.i(TAG, "Success: $response")
            }
            .onTransactionFailure { message ->
                Log.e(TAG, "Failure: $message")
            }

        setContent {
            MyApplicationTheme {
                var showCheckout by remember { mutableStateOf(false) }
                var showWebView by remember { mutableStateOf(false) }

                BackHandler(enabled = showWebView) {
                    showWebView = false
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("EdfaPay docs sample") },
                        )
                    },
                ) { innerPadding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        when {
                            showWebView -> {
                                EdfaWebView(
                                    onClose = { showWebView = false },
                                    onSuccess = { finalUrl ->
                                        Log.i(TAG, "3DS success: $finalUrl")
                                        showWebView = false
                                    },
                                    onFailure = { finalUrl ->
                                        Log.e(TAG, "3DS failure: $finalUrl")
                                        showWebView = false
                                    },
                                )
                            }

                            showCheckout -> {
                                EdfaPayUi(
                                    openWebView = { showWebView = true },
                                    onDismiss = { showCheckout = false },
                                )
                            }

                            else -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = "Minimal host for README-style integration.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                    )
                                    Text(
                                        text = "Replace API key and baseUrl above, then start checkout.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 24.dp),
                                    )
                                    Button(onClick = { showCheckout = true }) {
                                        Text("Start checkout")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        private const val TAG = "EdfaPay"
    }
}

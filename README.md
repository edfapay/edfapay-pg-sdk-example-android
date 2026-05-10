# EdfaPay Payment Gateway SDK

Kotlin Multiplatform payment SDK (Compose Multiplatform UI) for **Android** and **iOS**.

- **Source:** [github.com/edfapay/paymentGateway](https://github.com/edfapay/paymentGateway)
- **Maven coordinates (Android):** `io.github.edfapay:payment-sdk-android:<version>`
- **Minimum Android SDK:** API **24**

---

## Android integration

Host apps must use **Jetpack Compose** (`ComponentActivity` + `setContent`) because the checkout UI is provided as `@Composable`s.

### 1. Quick start

#### 1.1 Dependency

Published releases are intended for **[Maven Central](https://central.sonatype.com/)** under `io.github.edfapay`. Until your version appears on Central, publish locally and point Gradle at `mavenLocal()` or another internal repository.

```kotlin
dependencies {
    implementation("io.github.edfapay:payment-sdk-android:0.0.1") // replace with your version
}
```

Your **app** module must also enable Compose and use a Kotlin / Compose toolchain compatible with your project (the SDK builds with Kotlin **2.3.x** and Compose Multiplatform aligned dependencies).

```kotlin
// build.gradle.kts (app)
android {
    buildFeatures.compose = true
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
```

#### 1.2 Manifest

```xml
<manifest>
    <uses-permission android:name="android.permission.INTERNET" />
    ...
</manifest>
```

#### 1.3 Initialize the SDK (`Application`)

Call **`EdfaPgSdk.init` once**, before any payment UI or programmatic API usage. Optionally turn on internal logging:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        EdfaPgSdk.enableLogs = true
        EdfaPgSdk.init(
            apiKey = "YOUR_X_API_KEY",
            baseUrl = "https://your-payment-api-host.example", // trailing slash optional; use env-specific URL
        )
    }
}
```

#### 1.4 Show card checkout + 3-D Secure WebView

Configure **`EdfaCardPay`** with order and payer, then present **`EdfaPayUi`**. When the flow needs authentication, **`openWebView`** runs: show **`EdfaWebView`** and hide the sheet (`onDismiss`).

```kotlin
class CheckoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            options = EdfaPgPayerOptions(
                middleName = "Middle",
                birthdate = LocalDate.parse("1987-03-30"),
                address2 = "District",
                state = "Riyadh",
            ),
        )

        EdfaCardPay.shared()
            .setOrder(order)
            .setPayer(payer)
            .setDesignType(EdfaPayDesignType.ONE) // ONE, TWO, THREE
            .setLanguage(EdfaPayLanguage.en)       // en, ar
            .setDebug(true)
            .setResultUrls(
                successUrl = "https://edfapay.com/process-completed",
                failureUrl = "https://edfapay.com/process-failed",
            )
            .onTransactionSuccess { _, response ->
                Log.i(TAG, "Success: $response")
            }
            .onTransactionFailure { _, message ->
                Log.e(TAG, "Failure: $message")
            )

        setContent {
            MaterialTheme {
                var showWebView by remember { mutableStateOf(false) }

                Box(Modifier.fillMaxSize()) {
                    if (!showWebView) {
                        EdfaPayUi(
                            openWebView = { showWebView = true },
                            onDismiss = { finish() }, // sheet dismissed
                        )
                    } else {
                        EdfaWebView(
                            onClose = { showWebView = false },
                            onSuccess = { finalUrl ->
                                Log.i(TAG, "3DS success final URL: $finalUrl")
                                showWebView = false
                                // parse finalUrl query/fragment in your app if needed
                            },
                            onFailure = { finalUrl ->
                                Log.e(TAG, "3DS failure final URL: $finalUrl")
                                showWebView = false
                            },
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "EdfaPay"
    }
}
```

Imports (typical):

- `com.edfapay.payment_gateway.app.core.EdfaPgSdk`
- `com.edfapay.payment_gateway.presentation.EdfaPayUi`
- `com.edfapay.payment_gateway.presentation.EdfaWebView`
- `com.edfapay.payment_gateway.presentation.payment.models.EdfaCardPay`
- `com.edfapay.payment_gateway.app.toolbox.EdfaPayDesignType`
- `com.edfapay.payment_gateway.app.toolbox.EdfaPayLanguage`
- `com.edfapay.payment_gateway.data.model.request.order.EdfaPgSaleOrder`
- `com.edfapay.payment_gateway.data.model.request.payer.EdfaPgPayer`
- `com.edfapay.payment_gateway.data.model.request.payer.EdfaPgPayerOptions`

---

### 2. Full implementation notes

#### 2.1 `EdfaCardPay` builder

| Builder call | Purpose |
|--------------|---------|
| `setOrder(order)` | Sale order id, amount, currency, description (validated server-side conventions apply). |
| `setPayer(payer)` | Customer fields; **`payer.validate()`** runs before sale; fix errors before tapping pay. |
| `setCard(card)` | Optional: supply `EdfaPgCard` to skip onboard card UI (programmatic PAN path). |
| `setDesignType` / `setLanguage` | UI theme and localization. |
| `setAuth(true)` | **Auth-only** capture when supported by backend flow. |
| `setRecurringInit(true)` | Sets **`recurringInit = Y`** on the card S2S sale body when enrolling cards for recurring. |
| `setRecurring(channelId, recurring)` | Hosted recurring / channel options used by payment construction. |
| `setExtras(list)` | Additional `Extra` entries on the sale payload when applicable. |
| `setDebug` | Verbose diagnostics (pair with **`EdfaPgSdk.enableLogs`** for infrastructure logs). |
| `setResultUrls` | URLs used when completing 3DS / collector redirects in **`EdfaWebView`**. |
| `onTransactionSuccess` / `onTransactionFailure` | Hosted payment outcome callbacks after sale / redirects as implemented in the SDK. |

#### 2.2 API key lifecycle

Use **`EdfaPgSdk.updateApiKey(newKey)`** if the key can rotate during a session. **`baseUrl`** is fixed for the process after **`init`**; changing it requires a new process/`Application` lifecycle.

#### 2.3 Programmatic API (`TransactionProvider`)

For dashboards, reconciliation, refunds, or custom sale flows outside the bundled UI:

```kotlin
lifecycleScope.launch {
    val tx = EdfaPgSdk.transactionProvider()

    val page = tx.fetchAll(transactionId = "ledger-transaction-uuid") // filter uses query `id=`
    // page?.contents — inspect rows (e.g. status, declineReason)

    tx.void(transactionId = "...")

    tx.recurring(
        recurringRequestBody = RecurringRequestBody(
            transactionId = "ledger-transaction-uuid",
            recurringToken = null, // set when you have it; or use resolveRecurringTokenFromTransaction
            amount = 1.0,
            order = Order(
                number = "order-ref-123",
                amount = 1.0,
                currency = "SAR",
                description = "Recurring charge",
            ),
        ),
        resolveRecurringTokenFromTransaction = true,
    )
}
```

Add imports for types you use (`InitiateRequestDto`, `SaleRequestDto`, `Order`, `RecurringRequestBody`, …) from **`com.edfapay.payment_gateway.data.model`** and related packages.

**Recurring:** complete a successful **sale** with **`setRecurringInit(true)`**, then call **`recurring`** (SDK posts to **`…/api/v2/payment-gateway/recurring`**). Use the ledger **transaction UUID** consistent with **`filterTransaction`**.

#### 2.4 Payer validation (email)

`EdfaPgPayer.validate()` enforces required fields and a **strict email pattern** aligned with SDK checks (no **`+`** in the local-part, plain ASCII mailbox pattern).

#### 2.5 3-D Secure / WebView behavior

Documented behavior for **`EdfaWebView`** (from SDK KDoc): when the user lands on **`successUrl` / failureUrl**, the SDK invokes **`onSuccess` / `onFailure`** with the **full loaded URL**. Your activity decides when to **`showWebView = false`** (often immediately after success).

---

## iOS Host App

### Swift (`Shared` / SPM module name depends on integration)

```swift
import Shared

@main
struct iOSApp: App {
    init() {
        EdfaPgSdk.shared.doInit(apiKey: "ADD API KEY HERE", baseUrl: "ADD BASE URL HERE")
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

### UIKit (`AppDelegate`)

```swift
import UIKit
import PaymentGateway

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        EdfaPgSdk.shared.doInit(
            apiKey: "YOUR_KEY",
            baseUrl: "https://app-api.edfapay.com"
        )
        return true
    }
}
```

### Apple Pay (Kotlin API used from KMP bridging)

See in-repo samples / iOS bridging for **`EdfaApplePay`** and **`EdfaApplePaySheet`**. Apple Pay uses your **merchant identifier** and Xcode capability (**Wallet / Apple Pay**); the certificate is tied to Apple Developer configuration, not passed as a file in SDK calls.

---

## Publish and consume (maintainers)

Step-by-step **Sonatype user token**, **GPG key creation**, and **`publishToMavenCentral`** checklist: **`docs/PUBLISH_MAVEN_CENTRAL.md`**.

### Repositories used in internal release flows

Typical split:

- **Source repo:** this repository — builds artifacts.
- **Dist repo (optional legacy):** can host Maven on `gh-pages`, SwiftPM zips on `main`, and GitHub Releases.

Default patterns are team-specific; set **`DIST_DIR`** and **`DIST_REPO_SLUG`** (e.g. `edfapay/<dist-repo>`).

### Script usage (`release_to_dist.sh`)

Update paths and slug for your clones:

```bash
mkdir -p ~/workspace && cd ~/workspace
git clone git@github.com:edfapay/paymentGateway.git
# optional: clone your dist artifact repo alongside
chmod +x paymentGateway/release_to_dist.sh
```

```bash
export DIST_DIR="$HOME/workspace/<dist-repo>"
export DIST_REPO_SLUG="edfapay/<dist-repo>"
./release_to_dist.sh 0.0.1
```

Maven Central replaces personal GitHub Pages Maven for **public Android consumers**:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.edfapay:payment-sdk-android:0.0.1")
}
```

SwiftPM may still consume a tagged **binary**/`Package.swift` workflow from your dist repo or published XCFramework URLs.

---

## Build the library (local)

From repository root:

```bash
chmod +x buildAndPublish   # if your wrapper uses it
./buildAndPublish
```

Publish artifacts:

```bash
./gradlew :library:publishToMavenCentral "-PVERSION=0.0.1" # Central — see docs/PUBLISH_MAVEN_CENTRAL.md
./gradlew :library:publishAllPublicationsToLocalRepoRepository # local file repo (tests only)
```

---

## License / support

Consult your EdfaPay integration agreement for SLA and branding. Maven POM carries **Apache-2.0** metadata for OSS publication; rely on contractual terms where they differ.

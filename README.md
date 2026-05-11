# EdfaPay Payment Gateway SDK

Kotlin Multiplatform payment SDK (Compose Multiplatform UI) for **Android** and **iOS**.

Helps developers to easily integrate EdfaPay Payment Gateway into their mobile application in a few structured steps.

- **Minimum Android SDK:** API **24**

---

Select platform below and follow the steps to start integration.

> ### [Native (Android)](#native-android)
>
> ### [Native (iOS)](#native-ios)

---

## Native (Android)

### Maven / Gradle

Published releases are intended for **[Maven Central](https://central.sonatype.com/)** under the namespace **`io.github.edfapay`**. Until your version appears on Central, publish locally and point Gradle at `mavenLocal()` or another internal repository (see [Publish and consume (maintainers)](#publish-and-consume-maintainers)).

The library is published as a **Kotlin Multiplatform** artifact; Android apps depend on the **Android** variant:

```kotlin
// build.gradle.kts (app)
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.edfapay:payment-sdk-android:0.0.1") // replace with your version
}
```

**Verify the exact artifact id** for your release on Central (search `io.github.edfapay`). Gradle coordinates in this repo use the base id `payment-sdk`; KMP publishing typically exposes `payment-sdk-android` for the Android target.

### Toolchain alignment (host app)

Your **app** module must use **Jetpack Compose** (`ComponentActivity` + `setContent`) because checkout is provided as `@Composable`s. Match a toolchain in the same ballpark as the SDK build:

| Component | Version used in this repo (reference) |
|-----------|----------------------------------------|
| Kotlin | **2.3.x** |
| Compose Multiplatform | **1.9.x** (JetBrains Compose BOM stack; see `gradle/libs.versions.toml`) |
| Android Gradle Plugin | **8.13.x** |
| JVM target (Android) | **17** |

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

### ProGuard / R8

The SDK does **not** ship a separate `consumer-rules.pro` today. If minification breaks reflection or serialization at runtime, capture a stack trace and **open an issue** with your R8 full mode / app settings.

### 1. Manifest

**Network** and register your **`Application`** subclass if you initialize the SDK there:

```xml
<manifest>
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".MyApp"
        ...>
        ...
    </application>
</manifest>
```

### 2. Initialize the SDK (`Application`)

Call **`EdfaPgSdk.init` once**, before any payment UI or programmatic API usage. Optional logging:

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

### 3. Result URLs (3-D Secure / redirects)

Pass **`successUrl`** and **`failureUrl`** via **`EdfaCardPay.setResultUrls`**. The hosted flow and **`EdfaWebView`** complete when the user lands on one of these URLs. Use the **HTTPS return URLs** your EdfaPay integration or backend documents (they must be consistent with what the gateway expects for your merchant configuration). Your app may still parse query parameters on the final URL for order or transaction hints.

### 4. Show card checkout + 3-D Secure WebView

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
            .onTransactionSuccess { response ->
                Log.i(TAG, "Success: $response")
            }
            .onTransactionFailure { message ->
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

**Callbacks:** `onTransactionSuccess` receives a single `Any` payload; `onTransactionFailure` receives an optional `String?` message.

Typical imports:

- `com.edfapay.payment_gateway.app.core.EdfaPgSdk`
- `com.edfapay.payment_gateway.presentation.EdfaPayUi`
- `com.edfapay.payment_gateway.presentation.EdfaWebView`
- `com.edfapay.payment_gateway.presentation.payment.models.EdfaCardPay`
- `com.edfapay.payment_gateway.app.toolbox.EdfaPayDesignType`
- `com.edfapay.payment_gateway.app.toolbox.EdfaPayLanguage`
- `com.edfapay.payment_gateway.data.model.request.order.EdfaPgSaleOrder`
- `com.edfapay.payment_gateway.data.model.request.payer.EdfaPgPayer`
- `com.edfapay.payment_gateway.data.model.request.payer.EdfaPgPayerOptions`

### `EdfaCardPay` builder (reference)

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

### API key lifecycle

Use **`EdfaPgSdk.updateApiKey(newKey)`** if the key can rotate during a session. **`baseUrl`** is fixed for the process after **`init`**; changing it requires a new process / `Application` lifecycle.

### Programmatic API (`TransactionProvider`)

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

### Payer validation (email)

`EdfaPgPayer.validate()` enforces required fields and a **strict email pattern** aligned with SDK checks (no **`+`** in the local-part, plain ASCII mailbox pattern).

### 3-D Secure / WebView behavior

When the user reaches **`successUrl`** or **`failureUrl`**, **`EdfaWebView`** invokes **`onSuccess`** or **`onFailure`** with the **full loaded URL**. Your activity chooses when to hide the WebView (often immediately after handling the URL).

---

## Native (iOS)

The Kotlin CocoaPods integration in `:library` publishes the **`edfapg_sdk`** pod; the produced framework module name matches **`edfapg_sdk`** (see `cocoapods { name = "edfapg_sdk"; framework { baseName = "edfapg_sdk" } }` in `library/build.gradle.kts`).

Install via **CocoaPods** from your consuming app:

1. Ensure the `:library` project has produced the framework dummy or run `./gradlew :library:generateDummyFramework` before the first **`pod install`**, per the podspec instructions in-repo.
2. Point your **`Podfile`** at this repository (see `cocoapods` `extraSpecAttributes["source"]` in `library/build.gradle.kts`).

SwiftUI — initialize before UI:

```swift
import edfapg_sdk

@main
struct iOSApp: App {
    init() {
        EdfaPgSdk.shared.doInit(apiKey: "YOUR_API_KEY", baseUrl: "YOUR_BASE_URL")
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

UIKit — `AppDelegate`:

```swift
import UIKit
import edfapg_sdk

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        EdfaPgSdk.shared.doInit(
            apiKey: "YOUR_KEY",
            baseUrl: "https://app-api.example.com"
        )
        return true
    }
}
```

**Note:** Older samples may use **`import PaymentGateway`** when integrating via a **`EdfapayPG`** CocoaPods umbrella with `PRODUCT_MODULE_NAME = PaymentGateway`. Prefer **`edfapg_sdk`** for new apps aligned with the current Gradle Cocoapods name.

Compose Multiplatform UI entry points mirror the Kotlin APIs; expose them through your shared KMP **`shared`** Xcode framework if your app wraps the SDK in an umbrella module—in that setup **`import Shared`** only applies to **your** module name, not the SDK vendor name.

### Apple Pay

Apple Pay flows use **`EdfaApplePay`** / **`EdfaApplePaySheet`** from Kotlin APIs bridged into Swift. Enable **Apple Pay** capability and your **merchant identifier** in Xcode. See in-repo wiring and certificates on the Apple Developer account.

---

## Publish and consume (maintainers)

Step-by-step **Sonatype user token**, **GPG key creation**, and **`publishToMavenCentral`** checklist: **[`docs/PUBLISH_MAVEN_CENTRAL.md`](docs/PUBLISH_MAVEN_CENTRAL.md)**.

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
chmod +x paymentGateway/release_to_dist.sh
```

```bash
export DIST_DIR="$HOME/workspace/<dist-repo>"
export DIST_REPO_SLUG="edfapay/<dist-repo>"
./release_to_dist.sh 0.0.1
```

**Android:** Maven Central is the default distribution for public consumers (`mavenCentral()` + `implementation("io.github.edfapay:payment-sdk-android:…")`). **SwiftPM** may consume a tagged **XCFramework** or binary workflow separately.

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

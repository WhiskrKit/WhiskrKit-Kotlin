![WhiskrKit logo](https://whiskrkit.eu/WhiskrKit_logo.png)
# WhiskrKit for Android (Kotlin) — The purr-fect feedback toolkit for modern apps.

![version](https://img.shields.io/badge/version-0.1.0-blue) ![MIT](https://img.shields.io/badge/license-MIT-green)

WhiskrKit provides a flexible and easy-to-use API for presenting questionnaires and
feedback forms in your Jetpack Compose applications. This is the native Android
counterpart of [WhiskrKit for iOS](https://whiskrkit.eu); both SDKs talk to the same
backend and share the same survey templates.

## Features

* **Multiple question types**: star ratings, thumbs up/down, NPS scales, free text, multiple choice
* **Flexible presentation styles**: bottom sheets, banners, full-screen forms
* **Material 3 native**: zero-config theming derives from your `MaterialTheme`, including dark mode and dynamic color; everything is overridable
* **Offline-safe**: failed submissions queue locally and retry on foreground and when connectivity returns
* **State restoration**: open surveys and partial answers survive rotation and process death
* **Accessibility first**: native TalkBack semantics on every component

## Requirements

* minSdk 26 (Android 8.0)
* Jetpack Compose (Material 3)

## Installation

```kotlin
dependencies {
    implementation("eu.whiskrkit:whiskrkit-android:0.1.0")
}
```

## Quick start

**1. Initialize** in your `Application.onCreate()`. Get the API key from the WhiskrKit
dashboard you created for your app. Pass `withMockedSurveys = true` to explore the SDK
with built-in templates before wiring up a real key (see the `sample/` app).

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WhiskrKit.initialize(context = this, apiKey = "your-api-key")
    }
}
```

**2. Add the host** once, near the root of your composition, inside your `MaterialTheme`:

```kotlin
setContent {
    MyAppTheme {
        WhiskrKitHost {
            AppContent()
        }
    }
}
```

All surveys render at the host, whichever way they are triggered.

## Presenting surveys

### Automatic presentation

Place `WhiskrKitSurvey` on the screen where the survey should potentially appear.
WhiskrKit evaluates eligibility against the rules you configure in the dashboard
(session count, time intervals, audience percentage, …) and presents the survey
if the user qualifies:

```kotlin
@Composable
fun HomeScreen() {
    WhiskrKitSurvey(identifier = "your-survey-id")
    // ... screen content
}
```

### Event-driven trigger with backend targeting (hybrid)

When the timing is yours but the targeting decision should stay with the backend —
for example after a flow completes:

```kotlin
WhiskrKit.checkAndPresent(surveyId = "settings-feedback")
```

### Manual presentation

Full control — a feedback button, a push notification handler, any in-app trigger.
No eligibility check is performed:

```kotlin
Button(onClick = { WhiskrKit.present(surveyId = "your-survey-id") }) {
    Text("Give feedback")
}
```

> A `WhiskrKitHost` must be in the composition for surveys to appear. A trigger
> fired before the host exists is delivered to the first host that appears.

## Theming

Out of the box, surveys match your Material 3 theme — colors, typography, dark
mode, and dynamic color — with no configuration. To override selectively, pass a
`WhiskrKitTheme` to the host; anything you leave unset keeps its Material default:

```kotlin
WhiskrKitHost(
    theme = WhiskrKitTheme(
        title = WhiskrKitTheme.TextTheme(color = MyBrand.ink),
        button = WhiskrKitTheme.ButtonTheme(
            primary = WhiskrKitTheme.ButtonAppearance.Variant(
                WhiskrKitTheme.ButtonVariant(
                    backgroundColor = MyBrand.accent,
                    textColor = Color.White,
                    cornerRadius = 24.dp,
                ),
            ),
        ),
    ),
) { AppContent() }
```

For complete control over buttons there is a composable escape hatch:
`WhiskrKitTheme.ButtonAppearance.Custom { text, enabled, onClick -> ... }`.

## Permissions

The SDK's manifest merges two normal-level permissions into your app:
`INTERNET` (backend communication) and `ACCESS_NETWORK_STATE` (used to retry
queued submissions when connectivity returns).

## Differences from the iOS SDK

* "Toasts" are called **banners** on Android (same wire format, different name —
  `android.widget.Toast` is something else entirely).
* Sessions are counted per app-foreground, not per process launch, which is the
  more meaningful definition on Android. Counts may differ slightly from iOS for
  the same user behaviour.
* A `present()` call made before the host is composed is **delivered** once the
  host appears (iOS drops it).

## Sample app

The `sample/` module demonstrates every presentation mode and question type in
mock mode — no API key required. Open the project in Android Studio and run
the `sample` configuration.

## Development

```bash
./gradlew :whiskrkit:testDebugUnitTest        # unit tests (incl. architecture rules)
./gradlew :whiskrkit:verifyRoborazziDebug     # screenshot tests against golden images
./gradlew :whiskrkit:recordRoborazziDebug     # re-record golden images
./gradlew apiCheck                            # public API compatibility check
```

## Platform compatibility

* Android 8.0+ (API 26+)

## License

WhiskrKit is available under the MIT license. See [LICENSE.md](LICENSE.md).
Bundled icon path data is from Google's Material Icons (Apache 2.0) — see [NOTICE](NOTICE).

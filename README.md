![WhiskrKit logo](https://whiskrkit.eu/WhiskrKit_logo.png)
# WhiskrKit for Android (Kotlin) - The purr-fect feedback toolkit for modern apps.

![version](https://img.shields.io/badge/version-0.2.0-blue) ![MIT](https://img.shields.io/badge/license-MIT-green)

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

* minSdk 24 (Android 7.0)
* Jetpack Compose 1.10 or later (Material 3)

## Installation

```kotlin
dependencies {
    implementation("eu.whiskrkit:whiskrkit-android:0.2.0")
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

## Survey attributes

Attach context about the user to every response, for example their plan, to
segment results in the dashboard. Values are a string, number or boolean:

```kotlin
WhiskrKit.setAttribute("plan", "pro")
WhiskrKit.setAttribute("sessions", 3)
WhiskrKit.setAttribute("beta", true)

WhiskrKit.setAttributes(mapOf("plan" to "pro", "sessions" to 3))
WhiskrKit.removeAttribute("beta")
WhiskrKit.clearAttributes()
```

Attributes are global and held in memory only, so set them again at launch.
They can be set from any thread. The values in place when the user submits are
the ones sent, and a response that has to be retried keeps its attributes.

Limits: 20 attributes, keys of 1 to 64 characters, text values up to 256
characters, 2 KB in total. The SDK trims anything beyond that.

> **Do not send personal data.** Emails, phone numbers, names, IBANs and card
> numbers are dropped by the server, and attributes with keys such as `email`,
> `phone`, `name` or `address` are dropped too. In debug builds the SDK logs a
> warning when an attribute looks like personal data.

## Survey completion

Set a listener to hear when the user submits a survey, for example to log it in
your own analytics or to move on in a flow:

```kotlin
WhiskrKit.setOnSurveyCompletedListener { completion ->
    analytics.log("survey_completed", completion.surveyId)
    completion.answers.forEach { answer ->
        Log.d("Survey", "${answer.questionId} ${answer.questionType} ${answer.value}")
    }
}
```

`SurveyCompletion` holds the survey id and the submitted answers, each with its
question id, question type (`SCALE_RATING`, `SYMBOL_RATING`, `THUMBS_RATING`,
`TEXTUAL_SURVEY` or `MULTIPLE_CHOICE`) and value (`Value.Score`, `Value.Thumbs`,
`Value.Text` or `Value.Choices`). Questions left unanswered are not included.

The listener is called on the main thread as soon as the user submits, whether or
not the response reaches the server (a failed submission is queued and retried).
It is not called when a survey is dismissed without submitting, and it is also
called in mock mode. Pass `null` to remove it.

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

The `sample/` module is the WhiskrKit Playground: one screen to present every
question type and presentation style in mock mode (no API key required), change
the survey colors with presets or per color, preview them in light and dark
mode, and copy a Kotlin snippet that builds the same `WhiskrKitTheme`. Open the
project in Android Studio and run the `sample` configuration. The playground is
described in [sample/PLAYGROUND_SPEC.md](sample/PLAYGROUND_SPEC.md).

## Development

```bash
./gradlew :whiskrkit:testDebugUnitTest        # unit tests (incl. architecture rules)
./gradlew :whiskrkit:verifyRoborazziDebug     # screenshot tests against golden images
./gradlew :whiskrkit:recordRoborazziDebug     # re-record golden images
./gradlew apiCheck                            # public API compatibility check
./gradlew :sample:testDebugUnitTest           # playground snippet and persistence tests
```

## Platform compatibility

* Android 7.0+ (API 24+)

## License

WhiskrKit is available under the MIT license. See [LICENSE.md](LICENSE.md).
Bundled icon path data is from Google's Material Icons (Apache 2.0) — see [NOTICE](NOTICE).

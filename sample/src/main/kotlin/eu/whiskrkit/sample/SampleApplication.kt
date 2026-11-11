package eu.whiskrkit.sample

import android.app.Application
import android.content.Context
import eu.whiskrkit.WhiskrKit
import eu.whiskrkit.review.PlayReviewRequester

class SampleApplication : Application() {

    /** Loaded before any UI, so saved colors apply to the first survey. */
    internal lateinit var themeSettings: ThemeSettings
        private set

    override fun onCreate() {
        super.onCreate()
        // Mock mode: serves built-in surveys and only logs responses and events.
        // Any API key works. The Play review requester shows the wiring; in mock
        // mode a 4 or 5 star rating on `feature-feedback` only logs the request.
        WhiskrKit.initialize(
            context = this,
            apiKey = "playground",
            withMockedSurveys = true,
            reviewRequester = PlayReviewRequester(),
        )
        // Global attributes ride along with every response (visible in the mock log).
        WhiskrKit.setAttribute("plan", "playground")
        themeSettings = ThemeSettings(getSharedPreferences(ThemeSettings.PREFERENCES_NAME, Context.MODE_PRIVATE))
    }
}

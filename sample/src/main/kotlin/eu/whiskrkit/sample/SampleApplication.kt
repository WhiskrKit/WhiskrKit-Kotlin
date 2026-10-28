package eu.whiskrkit.sample

import android.app.Application
import android.content.Context
import eu.whiskrkit.WhiskrKit

class SampleApplication : Application() {

    /** Loaded before any UI, so saved colors apply to the first survey. */
    internal lateinit var themeSettings: ThemeSettings
        private set

    override fun onCreate() {
        super.onCreate()
        // Mock mode: serves built-in surveys and only logs responses and events.
        // Any API key works.
        WhiskrKit.initialize(
            context = this,
            apiKey = "playground",
            withMockedSurveys = true,
        )
        themeSettings = ThemeSettings(getSharedPreferences(ThemeSettings.PREFERENCES_NAME, Context.MODE_PRIVATE))
    }
}

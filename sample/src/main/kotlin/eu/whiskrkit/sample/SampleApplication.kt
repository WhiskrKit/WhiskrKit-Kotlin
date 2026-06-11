package eu.whiskrkit.sample

import android.app.Application
import eu.whiskrkit.WhiskrKit

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Mock mode: serves built-in templates, logs submissions. Swap in a real
        // API key and drop withMockedSurveys to hit the WhiskrKit backend.
        WhiskrKit.initialize(
            context = this,
            apiKey = "sample-key",
            withMockedSurveys = true,
        )
    }
}

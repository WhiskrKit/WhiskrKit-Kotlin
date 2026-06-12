package eu.whiskrkit

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import eu.whiskrkit.core.ConfigurationService
import eu.whiskrkit.core.MockConfigurationService
import eu.whiskrkit.core.MockEligibilityService
import eu.whiskrkit.core.PendingSurveyRequest
import eu.whiskrkit.core.WhiskrKitConfigurationService
import eu.whiskrkit.core.eligibility.EligibilityService
import eu.whiskrkit.core.eligibility.EligibilityStorage
import eu.whiskrkit.core.eligibility.SharedPrefsEligibilityStorage
import eu.whiskrkit.core.eligibility.WhiskrKitEligibilityService
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.model.SurveyTemplate
import eu.whiskrkit.core.network.NetworkService
import eu.whiskrkit.core.queue.SharedPrefsSubmissionStorage
import eu.whiskrkit.core.queue.SubmissionQueue
import eu.whiskrkit.internal.DeviceInfo
import eu.whiskrkit.internal.WhiskrLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.Instant

/**
 * Entry point of the WhiskrKit SDK.
 *
 * Call [initialize] once from your `Application.onCreate()`, place
 * [eu.whiskrkit.ui.WhiskrKitHost] near the root of your compose hierarchy, and
 * trigger surveys either automatically with [eu.whiskrkit.ui.WhiskrKitSurvey],
 * imperatively with [present], or with backend targeting via [checkAndPresent].
 */
public object WhiskrKit {

    private val baseUrl = "https://app.whiskrkit.eu".toHttpUrl()

    /** SDK-owned scope; outlives any composable so submissions survive dismissal. */
    internal val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    internal var configurationService: ConfigurationService? = null
    internal var eligibilityService: EligibilityService? = null
    internal var eligibilityStorage: EligibilityStorage? = null

    /**
     * The imperative trigger channel observed by the host: a StateFlow with
     * consume-and-null semantics. A `present()` call made before any host is
     * composed is buffered and delivered to the first host that appears.
     */
    internal val pendingSurvey: MutableStateFlow<PendingSurveyRequest?> = MutableStateFlow(null)

    /** Auto-trigger dedup: applies to [eu.whiskrkit.ui.WhiskrKitSurvey] only. */
    internal val autoCheckedIdentifiers: MutableSet<String> = mutableSetOf()

    private var lifecycleObserversRegistered = false

    /**
     * Initializes WhiskrKit with the provided API key.
     *
     * Must be called before any other WhiskrKit API, typically from
     * `Application.onCreate()` on the main thread.
     *
     * @param context any context; only the application context is retained.
     * @param apiKey your WhiskrKit API key from the dashboard.
     * @param withMockedSurveys when true, the SDK serves built-in mock surveys and
     *   logs submissions instead of calling the backend. Use any string as the key.
     */
    public fun initialize(
        context: Context,
        apiKey: String,
        withMockedSurveys: Boolean = false,
    ) {
        val appContext = context.applicationContext
        autoCheckedIdentifiers.clear()

        if (withMockedSurveys) {
            val mockService = MockConfigurationService()
            configurationService = mockService
            eligibilityService = MockEligibilityService(mockService)
            eligibilityStorage = null
        } else {
            val storage = SharedPrefsEligibilityStorage(appContext)
            storage.initializeIfNeeded()

            val deviceInfo = DeviceInfo(appContext) { storage.deviceId }
            val networkService = NetworkService(baseUrl, deviceInfo)
            val queue = SubmissionQueue(SharedPrefsSubmissionStorage(appContext))
            val service = WhiskrKitConfigurationService(networkService, queue, scope)
            service.configure(apiKey)

            configurationService = service
            eligibilityService = WhiskrKitEligibilityService(networkService, storage, deviceInfo)
            eligibilityStorage = storage
        }

        registerLifecycleObservers(appContext)
    }

    /**
     * Imperatively presents a survey, bypassing eligibility checks — for example
     * from a feedback button or a push notification handler.
     *
     * A [eu.whiskrkit.ui.WhiskrKitHost] must be active in the composition for the
     * survey to appear; a call made before the host exists is delivered once it does.
     */
    public fun present(surveyId: String) {
        pendingSurvey.value = PendingSurveyRequest.Fetch(surveyId)
    }

    /**
     * Checks eligibility for a survey and presents it only if the user qualifies.
     *
     * Use this when the timing is yours (after a flow completes, a screen closes)
     * but the targeting decision should stay with the backend. Unlike [present],
     * this respects eligibility rules; unlike [eu.whiskrkit.ui.WhiskrKitSurvey],
     * the moment it fires is entirely up to you.
     */
    public fun checkAndPresent(surveyId: String) {
        scope.launch {
            val template = checkEligibility(surveyId) ?: return@launch
            pendingSurvey.value = PendingSurveyRequest.Present(template)
        }
    }

    // region Internal API used by the UI layer

    internal suspend fun checkEligibility(surveyId: String): SurveyTemplate? {
        val service = eligibilityService ?: run {
            WhiskrLog.e(WhiskrLog.CORE, "WhiskrKit is not initialized. Call initialize() first.")
            return null
        }
        return service.checkEligibility(surveyId)
    }

    /** Auto-trigger path used by `WhiskrKitSurvey`; deduped per process. */
    internal suspend fun autoCheckAndPresent(identifier: String) {
        if (!autoCheckedIdentifiers.add(identifier)) return
        val template = checkEligibility(identifier) ?: return
        pendingSurvey.value = PendingSurveyRequest.Present(template)
    }

    internal suspend fun fetchSurveyTemplate(identifier: String): SurveyTemplate? {
        val service = configurationService ?: run {
            WhiskrLog.e(WhiskrLog.CORE, "WhiskrKit is not initialized. Call initialize() first.")
            return null
        }
        return service.fetchSurveyTemplate(identifier)
    }

    internal suspend fun submitSurveyResponse(surveyId: String, response: SurveyResponse) {
        val service = configurationService ?: run {
            WhiskrLog.e(WhiskrLog.CORE, "WhiskrKit is not initialized. Call initialize() first.")
            return
        }
        val success = service.submitSurveyResponse(surveyId, response)
        if (success) {
            trackSurveyCompletion(surveyId)
        }
    }

    // endregion

    private fun trackSurveyCompletion(surveyId: String) {
        val storage = eligibilityStorage ?: return
        storage.completedSurveys = storage.completedSurveys + (surveyId to Instant.now())
        storage.setNextCheckAfter(null, surveyId)
        WhiskrLog.i(WhiskrLog.CORE, "Tracked completion for survey '$surveyId'")
    }

    private fun registerLifecycleObservers(appContext: Context) {
        if (lifecycleObserversRegistered) return
        lifecycleObserversRegistered = true

        // Session = app came to foreground; also retries queued submissions.
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    eligibilityStorage?.incrementSessionCount()
                    scope.launch { configurationService?.retryPendingSubmissions() }
                }
            },
        )

        // Retry when connectivity returns. Application-scoped; never
        // unregistered on purpose.
        runCatching {
            val connectivityManager =
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager?.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        scope.launch { configurationService?.retryPendingSubmissions() }
                    }
                },
            )
        }.onFailure {
            WhiskrLog.w(WhiskrLog.CORE, "Could not register connectivity callback", it)
        }
    }
}

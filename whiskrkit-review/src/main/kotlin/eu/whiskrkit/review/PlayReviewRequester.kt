package eu.whiskrkit.review

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import eu.whiskrkit.ReviewRequester
import kotlin.coroutines.cancellation.CancellationException

// Same tag as the core SDK's log, so filtering on "WhiskrKit" shows both.
private const val TAG = "WhiskrKit"

/**
 * Shows the Google Play in-app review for WhiskrKit's store review follow-up.
 *
 * Pass it once at startup:
 * ```
 * WhiskrKit.initialize(this, apiKey, reviewRequester = PlayReviewRequester())
 * ```
 *
 * Google Play decides whether the review prompt actually appears (it applies its
 * own quota and never says whether it did), and it only works for apps installed
 * from Google Play. When the flow fails, the failure is logged; WhiskrKit never
 * opens the Play Store page instead.
 */
public class PlayReviewRequester internal constructor(
    private val reviewManager: (Context) -> ReviewManager,
) : ReviewRequester {

    public constructor() : this(ReviewManagerFactory::create)

    override suspend fun requestReview(activity: Activity) {
        try {
            val manager = reviewManager(activity.applicationContext)
            val reviewInfo = manager.requestReview()
            if (activity.isFinishing || activity.isDestroyed) {
                Log.i(TAG, "[Review] Activity went away; not launching the Play review flow")
                return
            }
            manager.launchReview(activity, reviewInfo)
            Log.i(TAG, "[Review] Play review flow finished")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "[Review] Play review flow failed", e)
        }
    }
}

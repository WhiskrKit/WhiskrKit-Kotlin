package eu.whiskrkit.review

import android.app.Activity
import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.testing.FakeReviewManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Counts calls; delegates to Play's own fake, or fails the request when [requestError] is set. */
private class CountingReviewManager(context: Context) : ReviewManager {
    private val fake = FakeReviewManager(context)
    var requestError: Exception? = null
    var requests = 0
    val launchedOn = mutableListOf<Activity>()

    override fun requestReviewFlow(): Task<ReviewInfo> {
        requests++
        return requestError?.let { Tasks.forException(it) } ?: fake.requestReviewFlow()
    }

    override fun launchReviewFlow(activity: Activity, reviewInfo: ReviewInfo): Task<Void> {
        launchedOn += activity
        return fake.launchReviewFlow(activity, reviewInfo)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayReviewRequesterTest {

    private val controller = Robolectric.buildActivity(Activity::class.java).setup()
    private val activity = controller.get()
    private val manager = CountingReviewManager(activity.applicationContext)
    private val requester = PlayReviewRequester { manager }

    @Test
    fun `requests and launches the Play review flow on the activity`() = runTest {
        requester.requestReview(activity)

        assertEquals(1, manager.requests)
        assertSame(activity, manager.launchedOn.single())
    }

    @Test
    fun `a failed request is logged and does not launch or throw`() = runTest {
        manager.requestError = IllegalStateException("Not installed from Play")

        requester.requestReview(activity)

        assertEquals(1, manager.requests)
        assertEquals(0, manager.launchedOn.size)
    }

    @Test
    fun `does not launch on an activity that finished meanwhile`() = runTest {
        activity.finish()

        requester.requestReview(activity)

        assertEquals(0, manager.launchedOn.size)
    }

    @Test
    fun `the public constructor builds without Play services`() {
        PlayReviewRequester()
    }
}

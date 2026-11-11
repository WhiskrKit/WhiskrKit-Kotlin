package eu.whiskrkit.core.review

import android.app.Activity
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ResumedActivityTrackerTest {

    private val application = RuntimeEnvironment.getApplication()
    private val tracker = ResumedActivityTracker()

    @Before
    fun setUp() {
        application.registerActivityLifecycleCallbacks(tracker)
    }

    @After
    fun tearDown() {
        application.unregisterActivityLifecycleCallbacks(tracker)
    }

    /** The tracker's only field; null means it holds no reference at all. */
    private fun heldReference(): WeakReference<*>? =
        ResumedActivityTracker::class.java.getDeclaredField("resumed")
            .apply { isAccessible = true }
            .get(tracker) as WeakReference<*>?

    @Test
    fun `tracks the resumed activity`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        assertSame(controller.get(), tracker.current)
    }

    @Test
    fun `releases the activity when it pauses`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        controller.pause()
        assertNull(tracker.current)
        assertNull(heldReference())
    }

    @Test
    fun `releases the activity when it is destroyed`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        controller.pause().stop().destroy()
        assertNull(tracker.current)
        assertNull(heldReference())
    }

    @Test
    fun `follows the newly resumed activity and ignores the old one pausing late`() {
        val first = Robolectric.buildActivity(Activity::class.java).setup()
        val second = Robolectric.buildActivity(Activity::class.java).setup()
        assertSame(second.get(), tracker.current)

        first.pause()
        assertSame(second.get(), tracker.current)
    }

    @Test
    fun `a finishing activity is not offered`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        controller.get().finish()
        assertNull(tracker.current)
    }
}

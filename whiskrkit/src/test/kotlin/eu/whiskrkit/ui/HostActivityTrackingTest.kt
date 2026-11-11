package eu.whiskrkit.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.whiskrkit.WhiskrKit
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * WhiskrKit.initialize is never called here, so the application-wide activity
 * callbacks are not registered: only the host can tell the tracker.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HostActivityTrackingTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a host composed in an already resumed activity reports it`() {
        rule.setContent { WhiskrKitHost {} }
        rule.waitForIdle()

        assertSame(rule.activity, WhiskrKit.activityTracker.current)
    }

    @Test
    fun `the host forgets its activity when it is destroyed`() {
        rule.setContent { WhiskrKitHost {} }
        rule.waitForIdle()

        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)

        assertNull(WhiskrKit.activityTracker.current)
    }
}

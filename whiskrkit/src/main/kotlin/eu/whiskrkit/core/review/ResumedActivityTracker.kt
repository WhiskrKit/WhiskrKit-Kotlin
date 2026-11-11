package eu.whiskrkit.core.review

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Remembers the activity that is currently resumed, so the store review prompt
 * can be launched on it. Holds a [WeakReference] only and forgets the activity
 * as soon as it pauses or is destroyed, so it never keeps an activity alive.
 */
internal class ResumedActivityTracker : Application.ActivityLifecycleCallbacks {

    private var resumed: WeakReference<Activity>? = null

    /** The resumed activity, or null when none is resumed or it is going away. */
    val current: Activity?
        get() = resumed?.get()?.takeUnless { it.isFinishing || it.isDestroyed }

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) = forget(activity)

    override fun onActivityDestroyed(activity: Activity) = forget(activity)

    private fun forget(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}

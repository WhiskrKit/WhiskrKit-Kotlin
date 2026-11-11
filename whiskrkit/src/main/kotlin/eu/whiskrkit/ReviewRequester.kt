package eu.whiskrkit

import android.app.Activity

/**
 * Asks the app store to show its in-app review prompt.
 *
 * WhiskrKit calls this after a survey was submitted successfully with a rating of
 * 4 or 5 stars on a star question that has the store review follow-up switched on
 * in the dashboard, at most once per app version. Pass an implementation to
 * [WhiskrKit.initialize]; the optional `eu.whiskrkit:whiskrkit-android-review`
 * artifact provides one for Google Play (`PlayReviewRequester`).
 *
 * Without a requester the SDK only logs that a review would have been requested,
 * so the core library carries no Google Play dependency.
 */
public interface ReviewRequester {

    /**
     * Requests the review prompt. Called on the main thread with the activity
     * that is resumed at that moment. The store decides whether a prompt
     * actually appears. Exceptions are caught and logged by WhiskrKit.
     */
    public suspend fun requestReview(activity: Activity)
}

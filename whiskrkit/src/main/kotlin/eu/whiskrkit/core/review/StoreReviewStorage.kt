package eu.whiskrkit.core.review

import android.content.Context
import android.content.SharedPreferences

/** Remembers the app version the store review was last requested for. */
internal interface StoreReviewStorage {
    var lastRequestedVersion: String?
}

internal class SharedPrefsStoreReviewStorage(
    private val prefs: SharedPreferences,
) : StoreReviewStorage {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE),
    )

    override var lastRequestedVersion: String?
        get() = prefs.getString(KEY_LAST_REQUESTED_VERSION, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_LAST_REQUESTED_VERSION)
                else putString(KEY_LAST_REQUESTED_VERSION, value)
            }.apply()
        }

    private companion object {
        // Same preferences file as the eligibility storage.
        const val PREFS_FILE = "eu.whiskrkit"
        const val KEY_LAST_REQUESTED_VERSION = "storeReviewRequestedVersion"
    }
}

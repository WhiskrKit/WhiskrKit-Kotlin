package eu.whiskrkit.internal

import android.util.Log

/**
 * Tiny internal logger with categories (Core / UI / Networking / Cache)
 * that avoids imposing a logging framework on host apps.
 */
internal object WhiskrLog {
    private const val TAG = "WhiskrKit"

    internal const val CORE = "Core"
    internal const val UI = "UI"
    internal const val NETWORKING = "Networking"
    internal const val CACHE = "Cache"

    fun i(category: String, message: String) {
        Log.i(TAG, "[$category] $message")
    }

    fun w(category: String, message: String, throwable: Throwable? = null) {
        Log.w(TAG, "[$category] $message", throwable)
    }

    fun e(category: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$category] $message", throwable)
    }
}

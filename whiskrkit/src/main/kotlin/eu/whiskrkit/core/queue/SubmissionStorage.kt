package eu.whiskrkit.core.queue

import android.content.Context
import android.content.SharedPreferences
import eu.whiskrkit.core.serialization.WireJson
import eu.whiskrkit.internal.WhiskrLog
import kotlinx.serialization.builtins.ListSerializer

internal interface SubmissionStorage {
    fun save(submissions: List<PendingSubmission>)
    fun load(): List<PendingSubmission>
    fun clear()
}

internal class SharedPrefsSubmissionStorage(
    private val prefs: SharedPreferences,
    private val key: String = SubmissionQueueConfig.STORAGE_KEY,
) : SubmissionStorage {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE),
    )

    override fun save(submissions: List<PendingSubmission>) {
        runCatching {
            val raw = WireJson.encodeToString(serializer, submissions)
            prefs.edit().putString(key, raw).apply()
            WhiskrLog.i(WhiskrLog.CACHE, "Saved ${submissions.size} submissions to storage")
        }.onFailure {
            WhiskrLog.w(WhiskrLog.CACHE, "Failed to save submissions", it)
        }
    }

    override fun load(): List<PendingSubmission> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            WireJson.decodeFromString(serializer, raw)
        }.onFailure {
            WhiskrLog.w(WhiskrLog.CACHE, "Failed to load submissions", it)
        }.getOrDefault(emptyList())
    }

    override fun clear() {
        prefs.edit().remove(key).apply()
        WhiskrLog.i(WhiskrLog.CACHE, "Cleared storage")
    }

    private companion object {
        const val PREFS_FILE = "eu.whiskrkit"
        val serializer = ListSerializer(PendingSubmission.serializer())
    }
}

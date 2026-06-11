package eu.whiskrkit.core.queue

import java.time.Instant

/**
 * In-memory queue with persistent backing, ported from iOS. Not thread-safe by
 * design: all access is confined to the SDK's main-dispatcher scope, mirroring
 * the iOS `@MainActor` isolation.
 */
internal class SubmissionQueue(
    private val storage: SubmissionStorage,
    private val now: () -> Instant = Instant::now,
) {
    private var submissions: MutableList<PendingSubmission> = mutableListOf()

    init {
        submissions = storage.load().toMutableList()
        cleanupExpired()
    }

    fun enqueue(submission: PendingSubmission) {
        submissions.removeAll { it.surveyId == submission.surveyId }
        if (submissions.size >= SubmissionQueueConfig.MAX_QUEUE_SIZE) {
            submissions.removeAt(0)
        }
        submissions.add(submission)
        persist()
    }

    fun dequeue(id: String) {
        submissions.removeAll { it.id == id }
        persist()
    }

    fun recordRetryAttempt(id: String) {
        val index = submissions.indexOfFirst { it.id == id }
        if (index >= 0) {
            submissions[index] = submissions[index].withRetryAttempt(now())
            persist()
        }
    }

    fun retryableSubmissions(): List<PendingSubmission> {
        cleanupExpired()
        return submissions.filter { it.canRetryNow(now()) }
    }

    fun getSubmission(id: String): PendingSubmission? =
        submissions.firstOrNull { it.id == id }

    val count: Int get() = submissions.size

    fun clear() {
        submissions.clear()
        persist()
    }

    private fun cleanupExpired() {
        val before = submissions.size
        submissions.removeAll { it.isExpired(now()) }
        if (before != submissions.size) {
            persist()
        }
    }

    private fun persist() {
        storage.save(submissions)
    }
}

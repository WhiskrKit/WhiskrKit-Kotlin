package eu.whiskrkit.core.eligibility

import android.content.Context
import android.content.SharedPreferences
import eu.whiskrkit.core.serialization.IsoInstantSerializer
import eu.whiskrkit.core.serialization.WireJson
import eu.whiskrkit.internal.WhiskrLog
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.time.Instant
import java.util.UUID

/**
 * Persistent state backing eligibility checks. The device id is a
 * self-generated UUID — app-scoped, reset on uninstall or clear-data,
 * no permissions required.
 */
internal interface EligibilityStorage {
    val deviceId: String
    val sessionCount: Int
    val installDate: Instant
    var lastSurveyDate: Instant?
    var completedSurveys: Map<String, Instant>

    /** Surveys that have been put on screen, whatever the outcome. */
    var seenSurveys: Map<String, Instant>

    fun nextCheckAfter(surveyId: String): Instant?
    fun setNextCheckAfter(date: Instant?, surveyId: String)
    fun removeCompletedSurvey(surveyId: String)
    fun removeSeenSurvey(surveyId: String)
    fun incrementSessionCount()

    /** Sets deviceId and installDate on first call; subsequent calls are no-ops. */
    fun initializeIfNeeded()
}

internal class SharedPrefsEligibilityStorage(
    private val prefs: SharedPreferences,
) : EligibilityStorage {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE),
    )

    private object Keys {
        const val DEVICE_ID = "deviceId"
        const val SESSION_COUNT = "sessionCount"
        const val INSTALL_DATE = "installDate"
        const val LAST_SURVEY_DATE = "lastSurveyDate"
        const val COMPLETED_SURVEYS = "completedSurveys"
        const val SEEN_SURVEYS = "seenSurveys"

        fun nextCheckAfter(surveyId: String) = "nextCheckAfter.$surveyId"
    }

    override val deviceId: String
        get() = prefs.getString(Keys.DEVICE_ID, null) ?: ""

    override val sessionCount: Int
        get() = prefs.getInt(Keys.SESSION_COUNT, 0)

    override val installDate: Instant
        get() = prefs.getLong(Keys.INSTALL_DATE, -1L)
            .takeIf { it >= 0 }
            ?.let(Instant::ofEpochMilli)
            ?: Instant.now()

    override var lastSurveyDate: Instant?
        get() = prefs.getLong(Keys.LAST_SURVEY_DATE, -1L)
            .takeIf { it >= 0 }
            ?.let(Instant::ofEpochMilli)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(Keys.LAST_SURVEY_DATE)
                else putLong(Keys.LAST_SURVEY_DATE, value.toEpochMilli())
            }.apply()
        }

    override var completedSurveys: Map<String, Instant>
        get() = dateMap(Keys.COMPLETED_SURVEYS)
        set(value) = setDateMap(Keys.COMPLETED_SURVEYS, value)

    override var seenSurveys: Map<String, Instant>
        get() = dateMap(Keys.SEEN_SURVEYS)
        set(value) = setDateMap(Keys.SEEN_SURVEYS, value)

    private fun dateMap(key: String): Map<String, Instant> {
        val raw = prefs.getString(key, null) ?: return emptyMap()
        return runCatching {
            WireJson.decodeFromString(dateMapSerializer, raw)
        }.getOrDefault(emptyMap())
    }

    private fun setDateMap(key: String, value: Map<String, Instant>) {
        val raw = WireJson.encodeToString(dateMapSerializer, value)
        prefs.edit().putString(key, raw).apply()
    }

    override fun nextCheckAfter(surveyId: String): Instant? =
        prefs.getLong(Keys.nextCheckAfter(surveyId), -1L)
            .takeIf { it >= 0 }
            ?.let(Instant::ofEpochMilli)

    override fun setNextCheckAfter(date: Instant?, surveyId: String) {
        prefs.edit().apply {
            if (date == null) remove(Keys.nextCheckAfter(surveyId))
            else putLong(Keys.nextCheckAfter(surveyId), date.toEpochMilli())
        }.apply()
    }

    override fun removeCompletedSurvey(surveyId: String) {
        completedSurveys = completedSurveys - surveyId
    }

    override fun removeSeenSurvey(surveyId: String) {
        seenSurveys = seenSurveys - surveyId
    }

    override fun incrementSessionCount() {
        val next = sessionCount + 1
        prefs.edit().putInt(Keys.SESSION_COUNT, next).apply()
        WhiskrLog.i(WhiskrLog.CORE, "Session count incremented to $next")
    }

    override fun initializeIfNeeded() {
        if (!prefs.contains(Keys.DEVICE_ID)) {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(Keys.DEVICE_ID, newId).apply()
            WhiskrLog.i(WhiskrLog.CORE, "Generated new device ID")
        }
        if (!prefs.contains(Keys.INSTALL_DATE)) {
            prefs.edit().putLong(Keys.INSTALL_DATE, Instant.now().toEpochMilli()).apply()
            WhiskrLog.i(WhiskrLog.CORE, "Install date set for the first time")
        }
    }

    private companion object {
        const val PREFS_FILE = "eu.whiskrkit"

        val dateMapSerializer =
            MapSerializer(String.serializer(), IsoInstantSerializer)
    }
}

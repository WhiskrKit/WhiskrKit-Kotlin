package eu.whiskrkit.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A point in time as milliseconds since the Unix epoch (UTC).
 *
 * Used instead of `java.time.Instant`, which needs API 26, so the SDK runs on
 * API 24 without asking apps to enable core library desugaring.
 */
@JvmInline
internal value class Timestamp(val epochMillis: Long) : Comparable<Timestamp> {
    override fun compareTo(other: Timestamp): Int = epochMillis.compareTo(other.epochMillis)

    fun isAfter(other: Timestamp): Boolean = epochMillis > other.epochMillis

    fun isBefore(other: Timestamp): Boolean = epochMillis < other.epochMillis

    fun plusSeconds(seconds: Long): Timestamp = Timestamp(epochMillis + seconds * MILLIS_PER_SECOND)

    fun minusSeconds(seconds: Long): Timestamp = plusSeconds(-seconds)

    fun plusDays(days: Long): Timestamp = Timestamp(epochMillis + days * MILLIS_PER_DAY)

    /** Whole seconds from [earlier] to this, rounded down like `Duration.getSeconds()`. */
    fun secondsSince(earlier: Timestamp): Long =
        (epochMillis - earlier.epochMillis).floorDiv(MILLIS_PER_SECOND)

    /** ISO-8601 in UTC with second precision, as the backend expects: `2026-06-11T12:00:00Z`. */
    fun toIsoString(): String {
        val seconds = epochMillis.floorDiv(MILLIS_PER_SECOND)
        val days = seconds.floorDiv(SECONDS_PER_DAY)
        val secondOfDay = seconds.mod(SECONDS_PER_DAY)
        val (year, month, day) = civilFromDays(days)
        return buildString(20) {
            appendPadded(year, 4)
            append('-')
            appendPadded(month.toLong(), 2)
            append('-')
            appendPadded(day.toLong(), 2)
            append('T')
            appendPadded(secondOfDay / 3600, 2)
            append(':')
            appendPadded(secondOfDay / 60 % 60, 2)
            append(':')
            appendPadded(secondOfDay % 60, 2)
            append('Z')
        }
    }

    override fun toString(): String = toIsoString()

    companion object {
        private const val MILLIS_PER_SECOND = 1_000L
        private const val SECONDS_PER_DAY = 86_400L
        private const val MILLIS_PER_DAY = SECONDS_PER_DAY * MILLIS_PER_SECOND

        private val ISO = Regex(
            """(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,9}))?(Z|[+-]\d{2}:\d{2})""",
        )

        fun now(): Timestamp = Timestamp(System.currentTimeMillis())

        fun ofEpochSecond(seconds: Long): Timestamp = Timestamp(seconds * MILLIS_PER_SECOND)

        /**
         * Parses an ISO-8601 date-time with a `Z` or `±hh:mm` offset and optional
         * fractional seconds (kept to the millisecond), like `Instant.parse`.
         *
         * @throws IllegalArgumentException when [text] is not such a date-time.
         */
        fun parse(text: String): Timestamp {
            val match = ISO.matchEntire(text) ?: throw IllegalArgumentException("Not an ISO-8601 instant: $text")
            val g = match.groupValues
            val year = g[1].toLong()
            val month = g[2].toInt()
            val day = g[3].toInt()
            val hour = g[4].toLong()
            val minute = g[5].toLong()
            val second = g[6].toLong()
            require(month in 1..12 && day in 1..daysInMonth(year, month)) { "Not a valid date: $text" }
            require(hour in 0..23 && minute in 0..59 && second in 0..59) { "Not a valid time: $text" }
            val millisOfSecond = g[7].padEnd(3, '0').take(3).toLong()
            val offsetSeconds = when (val offset = g[8]) {
                "Z" -> 0L
                else -> {
                    val sign = if (offset[0] == '-') -1 else 1
                    val hours = offset.substring(1, 3).toLong()
                    val minutes = offset.substring(4, 6).toLong()
                    require(hours <= 18 && minutes <= 59) { "Not a valid offset: $text" }
                    sign * (hours * 3600 + minutes * 60)
                }
            }
            val epochSeconds = daysFromCivil(year, month, day) * SECONDS_PER_DAY +
                hour * 3600 + minute * 60 + second - offsetSeconds
            return Timestamp(epochSeconds * MILLIS_PER_SECOND + millisOfSecond)
        }

        private fun isLeapYear(year: Long): Boolean = year % 4 == 0L && (year % 100 != 0L || year % 400 == 0L)

        private fun daysInMonth(year: Long, month: Int): Int = when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        /** Days since 1970-01-01 for a proleptic Gregorian date (Howard Hinnant's algorithm). */
        private fun daysFromCivil(year: Long, month: Int, day: Int): Long {
            val y = if (month <= 2) year - 1 else year
            val era = y.floorDiv(400L)
            val yearOfEra = y - era * 400
            val monthIndex = (month + 9) % 12
            val dayOfYear = (153 * monthIndex + 2) / 5 + day - 1
            val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
            return era * 146_097 + dayOfEra - 719_468
        }

        /** The proleptic Gregorian date for days since 1970-01-01. */
        private fun civilFromDays(days: Long): Triple<Long, Int, Int> {
            val z = days + 719_468
            val era = z.floorDiv(146_097L)
            val dayOfEra = z - era * 146_097
            val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
            val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
            val monthIndex = (5 * dayOfYear + 2) / 153
            val day = (dayOfYear - (153 * monthIndex + 2) / 5 + 1).toInt()
            val month = (if (monthIndex < 10) monthIndex + 3 else monthIndex - 9).toInt()
            val year = yearOfEra + era * 400 + if (month <= 2) 1 else 0
            return Triple(year, month, day)
        }

        private fun StringBuilder.appendPadded(value: Long, width: Int) {
            val text = value.toString()
            repeat(width - text.length) { append('0') }
            append(text)
        }
    }
}

/**
 * ISO-8601 serializer using the backend's date format
 * (`2026-06-11T12:00:00Z`, second precision, UTC). Reads the same forms as
 * `Instant.parse`, so data stored by earlier SDK versions still decodes.
 */
internal object IsoTimestampSerializer : KSerializer<Timestamp> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("eu.whiskrkit.IsoInstant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Timestamp) {
        encoder.encodeString(value.toIsoString())
    }

    override fun deserialize(decoder: Decoder): Timestamp {
        val text = decoder.decodeString()
        return try {
            Timestamp.parse(text)
        } catch (e: IllegalArgumentException) {
            throw SerializationException(e.message, e)
        }
    }
}

internal typealias IsoTimestamp = @Serializable(with = IsoTimestampSerializer::class) Timestamp

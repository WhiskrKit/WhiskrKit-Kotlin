package eu.whiskrkit.internal

import eu.whiskrkit.core.serialization.WireJson
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Timestamp replaces java.time.Instant (API 26) so the SDK runs on API 24. These
 * tests run on the JVM, where java.time is available, and check that formatting
 * and parsing match what the SDK did with Instant, so the wire format and data
 * stored by earlier SDK versions stay the same.
 */
class TimestampTest {

    /** What the SDK wrote with Instant before. */
    private fun oldFormat(millis: Long): String =
        DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(millis).truncatedTo(ChronoUnit.SECONDS))

    @Test
    fun `formats exactly like the Instant based serializer`() {
        val random = Random(42)
        val samples = listOf(0L, -1L, 999L, 1_000L, 951_782_400_000L, 4_102_444_799_999L) +
            List(5_000) { random.nextLong(-2_208_988_800_000L, 4_102_444_800_000L) }
        for (millis in samples) {
            assertEquals("millis=$millis", oldFormat(millis), Timestamp(millis).toIsoString())
        }
    }

    @Test
    fun `parses what Instant parses`() {
        val random = Random(7)
        repeat(5_000) {
            val millis = random.nextLong(-2_208_988_800_000L, 4_102_444_800_000L)
            val text = Instant.ofEpochMilli(millis).toString() // with fractional seconds when present
            assertEquals(text, Instant.parse(text).toEpochMilli(), Timestamp.parse(text).epochMillis)
        }
    }

    @Test
    fun `parses leap days, fractions and offsets`() {
        assertEquals(Instant.parse("2024-02-29T23:59:59Z").toEpochMilli(), Timestamp.parse("2024-02-29T23:59:59Z").epochMillis)
        assertEquals(Instant.parse("2026-06-11T12:00:00.5Z").toEpochMilli(), Timestamp.parse("2026-06-11T12:00:00.5Z").epochMillis)
        assertEquals(
            Instant.parse("2026-06-11T12:00:00.123456789Z").toEpochMilli(),
            Timestamp.parse("2026-06-11T12:00:00.123456789Z").epochMillis,
        )
        assertEquals(Timestamp.parse("2026-06-11T10:00:00Z"), Timestamp.parse("2026-06-11T12:00:00+02:00"))
        assertEquals(Timestamp.parse("2026-06-11T15:30:00Z"), Timestamp.parse("2026-06-11T12:00:00-03:30"))
    }

    @Test
    fun `rejects what is not an instant`() {
        for (bad in listOf("", "2026-06-11", "2026-06-11 12:00:00Z", "2026-02-30T00:00:00Z", "2025-02-29T00:00:00Z",
            "2026-13-01T00:00:00Z", "2026-06-11T24:00:00Z", "2026-06-11T12:00:00", "not a date")) {
            assertThrows(bad, IllegalArgumentException::class.java) { Timestamp.parse(bad) }
        }
    }

    @Test
    fun `serializer round-trips and reads data stored by earlier versions`() {
        val serializer = MapSerializer(String.serializer(), IsoTimestampSerializer)
        val stored = """{"checkout":"2026-06-11T12:30:45Z","onboarding":"2025-12-31T23:59:59Z"}"""
        val decoded = WireJson.decodeFromString(serializer, stored)
        assertEquals(Instant.parse("2026-06-11T12:30:45Z").toEpochMilli(), decoded.getValue("checkout").epochMillis)
        assertEquals(stored, WireJson.encodeToString(serializer, decoded))
        assertThrows(SerializationException::class.java) {
            WireJson.decodeFromString(serializer, """{"x":"yesterday"}""")
        }
    }

    @Test
    fun `arithmetic matches Instant and Duration`() {
        val start = Timestamp.parse("2026-06-11T12:00:00Z")
        assertEquals(Timestamp.parse("2026-06-18T12:00:00Z"), start.plusDays(7))
        assertEquals(Timestamp.parse("2026-06-11T12:01:30Z"), start.plusSeconds(90))
        assertTrue(start.plusSeconds(1).isAfter(start))
        assertFalse(start.isAfter(start))
        assertTrue(start.isBefore(start.plusSeconds(1)))
        for ((from, to) in listOf(0L to 1_999L, 1_999L to 0L, 500L to -500L, 0L to 60_000L)) {
            val expected = java.time.Duration.between(Instant.ofEpochMilli(from), Instant.ofEpochMilli(to)).seconds
            assertEquals("$from→$to", expected, Timestamp(to).secondsSince(Timestamp(from)))
        }
    }
}

package eu.whiskrkit.core

import eu.whiskrkit.core.serialization.WireJson
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttributeSanitizerTest {

    private fun text(s: String) = AttributeValue.Text(s)

    private fun sanitize(map: Map<String, AttributeValue>) = AttributeSanitizer.sanitize(map)

    private fun jsonBytes(map: Map<String, AttributeValue>): Int =
        WireJson.encodeToString(MapSerializer(String.serializer(), AttributeValueSerializer), map)
            .toByteArray(Charsets.UTF_8).size

    @Test
    fun `valid attributes pass through unchanged`() {
        val input = mapOf("plan" to text("pro"), "sessions" to AttributeValue.Integer(3), "beta" to AttributeValue.Bool(true))
        val result = sanitize(input)
        assertEquals(input, result.attributes)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `empty input stays empty`() {
        assertTrue(sanitize(emptyMap()).attributes.isEmpty())
    }

    @Test
    fun `keys are trimmed and blank keys dropped`() {
        val result = sanitize(mapOf(" plan " to text("pro"), "  " to text("x"), "" to text("y")))
        assertEquals(mapOf("plan" to text("pro")), result.attributes)
    }

    @Test
    fun `key of 64 characters is kept and 65 is dropped`() {
        val ok = "a".repeat(64)
        val tooLong = "b".repeat(65)
        val result = sanitize(mapOf(ok to text("1"), tooLong to text("2")))
        assertEquals(setOf(ok), result.attributes.keys)
    }

    @Test
    fun `string value is capped at 256 characters`() {
        val result = sanitize(mapOf("note" to text("x".repeat(300))))
        assertEquals("x".repeat(256), (result.attributes.getValue("note") as AttributeValue.Text).value)
    }

    @Test
    fun `string cap never splits a surrogate pair`() {
        val value = "a".repeat(255) + "🎉" // 255 chars + emoji straddling the limit
        val capped = (sanitize(mapOf("k" to text(value))).attributes.getValue("k") as AttributeValue.Text).value
        assertEquals("a".repeat(255), capped)
    }

    @Test
    fun `at most 20 keys are kept, dropping extras in sorted key order`() {
        val input = (1..21).associate { "k%02d".format(it) to AttributeValue.Integer(it.toLong()) }
        val result = sanitize(input)
        assertEquals(20, result.attributes.size)
        assertTrue("k21" !in result.attributes)
        assertTrue("k01" in result.attributes)
    }

    @Test
    fun `selection is deterministic regardless of insertion order`() {
        val keys = (1..25).map { "k%02d".format(it) }
        val a = sanitize(keys.associateWith { AttributeValue.Bool(true) })
        val b = sanitize(keys.reversed().associateWith { AttributeValue.Bool(true) })
        assertEquals(a.attributes.keys, b.attributes.keys)
    }

    @Test
    fun `result fits in 2048 bytes`() {
        val input = (1..20).associate { "key%02d".format(it) to text("v".repeat(256)) }
        val result = sanitize(input)
        // 20 x ~270 bytes is over the limit, so everything is dropped.
        assertTrue(jsonBytes(result.attributes) <= 2048)
        assertTrue(result.attributes.isEmpty())
    }

    @Test
    fun `payload exactly under the limit is kept`() {
        // 7 entries of ~270 bytes stay under 2048.
        val input = (1..7).associate { "key$it" to text("v".repeat(256)) }
        val result = sanitize(input)
        assertEquals(7, result.attributes.size)
        assertTrue(jsonBytes(result.attributes) <= 2048)
    }

    @Test
    fun `size is measured in utf-8 bytes not characters`() {
        // 256 euro signs = 256 chars but 768 bytes each. 3 of them exceed 2048 bytes.
        val input = (1..3).associate { "k$it" to text("€".repeat(256)) }
        val chars = WireJson.encodeToString(MapSerializer(String.serializer(), AttributeValueSerializer), input).length
        assertTrue("fits as chars", chars <= 2048)
        val result = sanitize(input)
        assertTrue(result.attributes.isEmpty())
    }

    @Test
    fun `personal data keys warn but are not stripped`() {
        val keys = listOf(
            "email", "userEmail", "e-mail", "phone", "Mobile_number", "iban", "bsn", "passport_no",
            "home_address", "creditCard", "name", "firstName", "last_name", "FULLNAME", "surname",
            "zip", "zipcode", "postcode", "postal_code",
        )
        val result = sanitize(keys.associateWith { text("x") })
        // Only 20 keys fit; all are within the limit here (19).
        assertEquals(keys.toSet(), result.attributes.keys)
        keys.forEach { key ->
            assertTrue("expected warning for $key", result.warnings.any { it.contains("'$key'") })
        }
    }

    @Test
    fun `harmless keys containing name fragments do not warn`() {
        val result = sanitize(
            mapOf("plan" to text("pro"), "username_set" to AttributeValue.Bool(true), "company" to text("x"), "filename" to text("a")),
        )
        assertTrue(result.warnings.toString(), result.warnings.isEmpty())
    }

    @Test
    fun `values that look like an email or iban warn without being logged`() {
        val result = sanitize(
            mapOf(
                "contact" to text("jane@example.com"),
                "account" to text("NL91 ABNA 0417 1643 00"),
                "plan" to text("pro"),
            ),
        )
        assertEquals(3, result.attributes.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.warnings.none { it.contains("jane@example.com") || it.contains("ABNA") })
    }

    @Test
    fun `a key that is an email warns`() {
        val result = sanitize(mapOf("jane@example.com" to AttributeValue.Bool(true)))
        assertEquals(1, result.warnings.size)
        assertTrue(result.attributes.isNotEmpty())
    }
}

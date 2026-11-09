package eu.whiskrkit.core

import eu.whiskrkit.core.serialization.WireJson
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

internal data class SanitizedAttributes(
    val attributes: Map<String, AttributeValue>,
    /** Debug hints about likely personal data. Never contain attribute values. */
    val warnings: List<String>,
)

/**
 * Mirrors the backend limits so a payload is never rejected for size and the
 * result is deterministic. The server stays the authority: personal-data hints
 * only warn, nothing is stripped here.
 */
internal object AttributeSanitizer {
    const val MAX_KEYS = 20
    const val MAX_KEY_LENGTH = 64
    const val MAX_STRING_LENGTH = 256
    const val MAX_JSON_BYTES = 2048

    private val personalKeyFragments = listOf(
        "email", "mail", "phone", "mobile", "iban", "bsn", "passport", "address", "creditcard",
    )
    private val personalKeysExact = setOf(
        "name", "firstname", "lastname", "fullname", "surname", "zip", "zipcode", "postcode", "postalcode",
    )
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val ibanPattern = Regex("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$")

    private val mapSerializer = MapSerializer(String.serializer(), AttributeValueSerializer)

    fun sanitize(input: Map<String, AttributeValue>): SanitizedAttributes {
        if (input.isEmpty()) return SanitizedAttributes(emptyMap(), emptyList())

        // Sorted by the trimmed key so which entries survive does not depend on insertion order.
        val cleaned = sortedMapOf<String, AttributeValue>()
        for ((rawKey, value) in input) {
            val key = rawKey.trim()
            if (key.isEmpty() || key.length > MAX_KEY_LENGTH) continue
            cleaned[key] = if (value is AttributeValue.Text) AttributeValue.Text(capString(value.value)) else value
        }

        val limited = LinkedHashMap<String, AttributeValue>()
        for ((key, value) in cleaned) {
            if (limited.size == MAX_KEYS) break
            limited[key] = value
        }

        val fits = jsonBytes(limited) <= MAX_JSON_BYTES
        val result: Map<String, AttributeValue> = if (fits) limited else emptyMap()
        return SanitizedAttributes(result, warnings(result))
    }

    private fun capString(value: String): String {
        if (value.length <= MAX_STRING_LENGTH) return value
        var end = MAX_STRING_LENGTH
        if (Character.isHighSurrogate(value[end - 1])) end--
        return value.substring(0, end)
    }

    private fun jsonBytes(attributes: Map<String, AttributeValue>): Int =
        WireJson.encodeToString(mapSerializer, attributes).toByteArray(Charsets.UTF_8).size

    private fun warnings(attributes: Map<String, AttributeValue>): List<String> {
        val out = mutableListOf<String>()
        for ((key, value) in attributes) {
            if (looksPersonal(key)) {
                out += "Attribute key '$key' looks like personal data. The server drops personal data, do not send it."
            } else if (value is AttributeValue.Text && looksLikeEmailOrIban(value.value)) {
                out += "The value of attribute '$key' looks like an email address or IBAN. The server drops personal data, do not send it."
            }
        }
        return out
    }

    private fun looksPersonal(key: String): Boolean {
        if (looksLikeEmailOrIban(key)) return true
        val letters = key.lowercase().filter { it in 'a'..'z' }
        return letters in personalKeysExact || personalKeyFragments.any { it in letters }
    }

    private fun looksLikeEmailOrIban(text: String): Boolean {
        val trimmed = text.trim()
        if (emailPattern.matches(trimmed)) return true
        val compact = trimmed.filterNot { it.isWhitespace() }.uppercase()
        return ibanPattern.matches(compact)
    }
}

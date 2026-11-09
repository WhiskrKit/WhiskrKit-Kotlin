package eu.whiskrkit.core

import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory holder of the global survey attributes. Safe to call from any
 * thread: it keeps an immutable map and swaps it with compare-and-set, so
 * concurrent writers never lose an update and readers get a consistent snapshot.
 * Not persisted: attributes describe the current session and are set again at launch.
 */
internal class AttributeStore {
    private val state = AtomicReference<Map<String, AttributeValue>>(emptyMap())

    fun set(key: String, value: AttributeValue) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return
        update { it + (trimmed to value) }
    }

    /** Merges the supported entries of [values]; unsupported types are skipped. */
    fun setAll(values: Map<String, Any?>) {
        val converted = LinkedHashMap<String, AttributeValue>()
        for ((key, raw) in values) {
            val trimmed = key.trim()
            val value = AttributeValue.of(raw)
            if (trimmed.isEmpty() || value == null) continue
            converted[trimmed] = value
        }
        if (converted.isNotEmpty()) update { it + converted }
    }

    fun remove(key: String) {
        val trimmed = key.trim()
        update { if (trimmed in it) it - trimmed else it }
    }

    fun clear() {
        state.set(emptyMap())
    }

    /** An immutable copy of the current attributes. */
    fun snapshot(): Map<String, AttributeValue> = state.get()

    private inline fun update(transform: (Map<String, AttributeValue>) -> Map<String, AttributeValue>) {
        while (true) {
            val current = state.get()
            if (state.compareAndSet(current, transform(current))) return
        }
    }
}

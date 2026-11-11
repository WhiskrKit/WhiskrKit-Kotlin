package eu.whiskrkit.core.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/**
 * The single Json configuration used for everything that crosses the wire or is
 * persisted. `ignoreUnknownKeys` keeps old SDK versions tolerant of new backend
 * fields; `explicitNulls = false` omits null fields, as the backend expects.
 */
@OptIn(ExperimentalSerializationApi::class)
internal val WireJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

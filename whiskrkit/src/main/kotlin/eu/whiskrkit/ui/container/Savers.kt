package eu.whiskrkit.ui.container

import androidx.compose.runtime.saveable.Saver
import eu.whiskrkit.core.model.BannerTemplate
import eu.whiskrkit.core.model.FullScreenFormTemplate
import eu.whiskrkit.core.model.SheetTemplate
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.model.SurveyTemplate
import eu.whiskrkit.core.serialization.WireJson

/**
 * Rotation/process-death survival (decision #9): the active template and the
 * answers round-trip through their JSON wire form into saved instance state.
 * Encoding uses the concrete serializer per type (no reflective lookup, R8-safe);
 * decoding goes through the polymorphic `template` discriminator.
 */
internal val SurveyTemplateSaver: Saver<SurveyTemplate?, String> = Saver(
    save = { value ->
        when (value) {
            null -> ""
            is SheetTemplate -> WireJson.encodeToString(SheetTemplate.serializer(), value)
            is FullScreenFormTemplate ->
                WireJson.encodeToString(FullScreenFormTemplate.serializer(), value)
            is BannerTemplate -> WireJson.encodeToString(BannerTemplate.serializer(), value)
        }
    },
    restore = { raw ->
        if (raw.isEmpty()) {
            null
        } else {
            runCatching {
                WireJson.decodeFromString(SurveyTemplate.serializer(), raw)
            }.getOrNull()
        }
    },
)

internal val SurveyResponseSaver: Saver<SurveyResponse, String> = Saver(
    save = { WireJson.encodeToString(SurveyResponse.serializer(), it) },
    restore = { raw ->
        runCatching {
            WireJson.decodeFromString(SurveyResponse.serializer(), raw)
        }.getOrDefault(SurveyResponse())
    },
)

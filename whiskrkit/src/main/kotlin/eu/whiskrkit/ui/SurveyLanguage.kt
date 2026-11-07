package eu.whiskrkit.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import java.util.Locale

/**
 * Shows WhiskrKit's own strings in the language the survey was served in, so
 * "Required", "Submit" and the TalkBack labels match the survey text. Without a
 * language from the backend, they follow the app's language.
 */
@Composable
internal fun SurveyLanguage(language: String?, content: @Composable () -> Unit) {
    if (language.isNullOrBlank()) {
        content()
        return
    }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localized = remember(context, configuration, language) { context.withLanguage(language, configuration) }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        LocalResources provides localized.resources,
    ) {
        content()
    }
}

/** This context with its resources in [language], a BCP 47 tag such as `nl` or `pt-BR`. */
internal fun Context.withLanguage(
    language: String,
    configuration: Configuration = resources.configuration,
): Context {
    val localizedConfiguration = Configuration(configuration).apply {
        setLocale(Locale.forLanguageTag(language))
    }
    return LocalizedContext(this, createConfigurationContext(localizedConfiguration).resources)
}

/** Keeps the original context chain, so the Activity stays reachable, but with localized resources. */
private class LocalizedContext(base: Context, private val localizedResources: Resources) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResources
}

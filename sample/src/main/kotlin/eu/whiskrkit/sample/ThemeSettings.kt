package eu.whiskrkit.sample

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import eu.whiskrkit.theme.WhiskrKitTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * The playground's theme colors. Every change is saved at once; the activity
 * passes [buildTheme] of the current colors to `WhiskrKitHost`, so the next
 * survey (and one already on screen) uses it.
 */
internal class ThemeSettings(private val preferences: SharedPreferences) {

    /** Custom colors (`#RRGGBB`) keyed by [ColorRole.id]. A role without an entry uses its Material default. */
    var customColors: Map<String, String> by mutableStateOf(decodeColors(preferences.getString(STORAGE_KEY, null)))
        private set

    /** The preset the current colors are equal to, if any. */
    val matchingPreset: ThemePreset?
        get() = ThemePreset.all.firstOrNull { it.colors == customColors }

    fun setColor(role: ColorRole, hex: String) = update(customColors + (role.id to hex))

    fun reset(role: ColorRole) = update(customColors - role.id)

    fun apply(preset: ThemePreset) = update(preset.colors)

    private fun update(colors: Map<String, String>) {
        customColors = colors
        preferences.edit().putString(STORAGE_KEY, encodeColors(colors)).apply()
    }

    companion object {
        const val PREFERENCES_NAME = "whiskrkit.playground"
        const val STORAGE_KEY = "whiskrkit.playground.colors"
    }
}

// region Theme and snippet: two views of the same mapping, change them together.

/**
 * The theme for [customColors], or null (the SDK default) when nothing is
 * customized. A role that isn't customized is `Color.Unspecified`, which the
 * SDK resolves to its Material default for each property.
 */
internal fun buildTheme(customColors: Map<String, String>): WhiskrKitTheme? {
    if (customColors.isEmpty()) return null

    fun color(role: ColorRole) = customColors[role.id]?.let(::hexToColor) ?: Color.Unspecified

    val accent = color(ColorRole.Accent)
    val onAccent = color(ColorRole.OnAccent)
    val background = color(ColorRole.Background)
    val text = color(ColorRole.Text)
    val secondaryText = color(ColorRole.SecondaryText)
    val selection = color(ColorRole.Selection)
    // Unselected options, thumbs and scale chips sit on the selection
    // background, with labels in the text color. Its default adapts to the
    // mode, so with a fixed background (and usually fixed text) it follows the
    // background instead, slightly raised.
    val selectionBackground = customColors[ColorRole.Background.id]
        ?.let { hexToColor(raisedHex(it)) } ?: Color.Unspecified

    return WhiskrKitTheme(
        container = WhiskrKitTheme.ContainerTheme(
            sheet = WhiskrKitTheme.OverlayTheme(backgroundColor = background),
            fullScreen = WhiskrKitTheme.OverlayTheme(backgroundColor = background),
            banner = WhiskrKitTheme.BannerTheme(backgroundColor = background),
        ),
        selection = WhiskrKitTheme.SelectionTheme(tintColor = selection, backgroundColor = selectionBackground),
        button = WhiskrKitTheme.ButtonTheme(
            primary = WhiskrKitTheme.ButtonAppearance.Variant(
                WhiskrKitTheme.ButtonVariant(backgroundColor = accent, textColor = onAccent),
            ),
            secondary = WhiskrKitTheme.ButtonAppearance.Variant(
                WhiskrKitTheme.ButtonVariant(textColor = accent),
            ),
        ),
        title = WhiskrKitTheme.TextTheme(color = text),
        subtitle = WhiskrKitTheme.TextTheme(color = secondaryText),
        headline = WhiskrKitTheme.TextTheme(color = text),
        subheadline = WhiskrKitTheme.TextTheme(color = secondaryText),
        body = WhiskrKitTheme.TextTheme(color = text),
    )
}

/**
 * Kotlin source that builds the same theme as [buildTheme] and passes it to
 * the host. It compiles inside any `@Composable` with these imports:
 * `androidx.compose.ui.graphics.Color`, `eu.whiskrkit.theme.WhiskrKitTheme`
 * and `eu.whiskrkit.ui.WhiskrKitHost`.
 */
internal fun themeSnippet(customColors: Map<String, String>): String {
    if (customColors.isEmpty()) {
        return """
            WhiskrKitHost {
                // Your app content
            }
        """.trimIndent()
    }

    val declarations = ColorRole.all.joinToString("\n") { role ->
        val hex = customColors[role.id]
        if (hex != null) {
            "val ${role.id} = Color(0xFF${hex.substring(1)})"
        } else {
            "val ${role.id} = Color.Unspecified // MaterialTheme default"
        }
    }

    val selectionTheme = customColors[ColorRole.Background.id]?.let { background ->
        "WhiskrKitTheme.SelectionTheme(tintColor = selection, backgroundColor = Color(0xFF${raisedHex(background).substring(1)})), // raised background"
    } ?: "WhiskrKitTheme.SelectionTheme(tintColor = selection),"

    return declarations + "\n\n" + """
        val theme = WhiskrKitTheme(
            container = WhiskrKitTheme.ContainerTheme(
                sheet = WhiskrKitTheme.OverlayTheme(backgroundColor = background),
                fullScreen = WhiskrKitTheme.OverlayTheme(backgroundColor = background),
                banner = WhiskrKitTheme.BannerTheme(backgroundColor = background),
            ),
            selection = $selectionTheme
            button = WhiskrKitTheme.ButtonTheme(
                primary = WhiskrKitTheme.ButtonAppearance.Variant(
                    WhiskrKitTheme.ButtonVariant(backgroundColor = accent, textColor = onAccent),
                ),
                secondary = WhiskrKitTheme.ButtonAppearance.Variant(
                    WhiskrKitTheme.ButtonVariant(textColor = accent),
                ),
            ),
            title = WhiskrKitTheme.TextTheme(color = text),
            subtitle = WhiskrKitTheme.TextTheme(color = secondaryText),
            headline = WhiskrKitTheme.TextTheme(color = text),
            subheadline = WhiskrKitTheme.TextTheme(color = secondaryText),
            body = WhiskrKitTheme.TextTheme(color = text),
        )

        WhiskrKitHost(theme = theme) {
            // Your app content
        }
    """.trimIndent()
}

// endregion

// region Persistence: JSON object of role id to "#RRGGBB", customized roles only.

/** Drops unknown role ids and values that aren't 6-digit hex. */
internal fun decodeColors(json: String?): Map<String, String> {
    val stored = json?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }
        ?: return emptyMap()
    val knownRoles = ColorRole.all.map { it.id }.toSet()
    return stored
        .filterKeys { it in knownRoles }
        .mapNotNull { (role, value) ->
            val hex = (value as? JsonPrimitive)?.takeIf { it.isString }?.content?.let(::normalizedHex)
            hex?.let { role to it }
        }
        .toMap()
}

/** Writes roles in [ColorRole.all] order. */
internal fun encodeColors(colors: Map<String, String>): String =
    JsonObject(
        ColorRole.all
            .mapNotNull { role -> colors[role.id]?.let { role.id to JsonPrimitive(it) } }
            .toMap(),
    ).toString()

// endregion

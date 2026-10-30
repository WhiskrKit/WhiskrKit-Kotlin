package eu.whiskrkit.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import eu.whiskrkit.theme.WhiskrKitTheme
import eu.whiskrkit.ui.WhiskrKitHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeSettingsTest {

    @Test
    fun encodesCustomizedRolesInRoleOrder() {
        val json = encodeColors(mapOf("selection" to "#0066CC", "accent" to "#0066CC"))
        assertEquals("""{"accent":"#0066CC","selection":"#0066CC"}""", json)
    }

    @Test
    fun decodeRoundTripsEncode() {
        val colors = ThemePreset.Midnight.colors
        assertEquals(colors, decodeColors(encodeColors(colors)))
    }

    @Test
    fun decodeDropsUnknownRolesAndInvalidValuesAndUppercases() {
        val json = """{"accent":"#0066cc","bogus":"#FFFFFF","text":"#FFF","background":12,"onAccent":"red"}"""
        assertEquals(mapOf("accent" to "#0066CC"), decodeColors(json))
    }

    @Test
    fun decodeTreatsMissingOrBrokenValueAsSystem() {
        assertEquals(emptyMap<String, String>(), decodeColors(null))
        assertEquals(emptyMap<String, String>(), decodeColors("not json"))
        assertEquals(emptyMap<String, String>(), decodeColors("[]"))
    }

    @Test
    fun noCustomColorsMeansNoTheme() {
        assertNull(buildTheme(emptyMap()))
        assertEquals(SYSTEM_SNIPPET, themeSnippet(emptyMap()))
    }

    @Test
    fun oceanSnippet() {
        assertEquals(OCEAN_SNIPPET, themeSnippet(ThemePreset.Ocean.colors))
    }

    @Test
    fun midnightSnippet() {
        assertEquals(MIDNIGHT_SNIPPET, themeSnippet(ThemePreset.Midnight.colors))
    }

    @Test
    fun raisedHexLightensDarkColorsAndDarkensLightOnes() {
        assertEquals("#323244", raisedHex("#1B1B2F"))
        assertEquals("#1A1A1A", raisedHex("#000000"))
        assertEquals("#E6E6E6", raisedHex("#FFFFFF"))
    }

    @Test
    fun hexConversionRoundTrips() {
        listOf("#000000", "#0066CC", "#F5C542", "#FFFFFF").forEach { hex ->
            assertEquals(hex, hexToColor(hex).toHex())
        }
    }
}

private val SYSTEM_SNIPPET = """
    WhiskrKitHost {
        // Your app content
    }
""".trimIndent()

private val OCEAN_SNIPPET = """
    val accent = Color(0xFF0066CC)
    val onAccent = Color(0xFFFFFFFF)
    val background = Color.Unspecified // MaterialTheme default
    val text = Color.Unspecified // MaterialTheme default
    val secondaryText = Color.Unspecified // MaterialTheme default
    val selection = Color(0xFF0066CC)

    val theme = WhiskrKitTheme(
        container = WhiskrKitTheme.ContainerTheme(
            sheet = WhiskrKitTheme.OverlayTheme(backgroundColor = background),
            fullScreen = WhiskrKitTheme.OverlayTheme(backgroundColor = background),
            banner = WhiskrKitTheme.BannerTheme(backgroundColor = background),
        ),
        selection = WhiskrKitTheme.SelectionTheme(tintColor = selection),
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

// Keep in sync with MidnightSnippetCompiles below.
private val MIDNIGHT_SNIPPET = """
    val accent = Color(0xFFF5C542)
    val onAccent = Color(0xFF1B1B2F)
    val background = Color(0xFF1B1B2F)
    val text = Color(0xFFF2F2F7)
    val secondaryText = Color(0xFFA5A5C0)
    val selection = Color(0xFFF5C542)

    val theme = WhiskrKitTheme(
        container = WhiskrKitTheme.ContainerTheme(
            sheet = WhiskrKitTheme.OverlayTheme(backgroundColor = background),
            fullScreen = WhiskrKitTheme.OverlayTheme(backgroundColor = background),
            banner = WhiskrKitTheme.BannerTheme(backgroundColor = background),
        ),
        selection = WhiskrKitTheme.SelectionTheme(tintColor = selection, backgroundColor = Color(0xFF323244)), // raised background
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

/**
 * The Midnight snippet pasted as code, with only the snippet's three imports.
 * It is never called: it exists so a change to the SDK's public theme API
 * breaks this test source set's compilation.
 */
@Suppress("unused")
@Composable
private fun MidnightSnippetCompiles() {
    val accent = Color(0xFFF5C542)
    val onAccent = Color(0xFF1B1B2F)
    val background = Color(0xFF1B1B2F)
    val text = Color(0xFFF2F2F7)
    val secondaryText = Color(0xFFA5A5C0)
    val selection = Color(0xFFF5C542)

    val theme = WhiskrKitTheme(
        container = WhiskrKitTheme.ContainerTheme(
            sheet = WhiskrKitTheme.OverlayTheme(backgroundColor = background),
            fullScreen = WhiskrKitTheme.OverlayTheme(backgroundColor = background),
            banner = WhiskrKitTheme.BannerTheme(backgroundColor = background),
        ),
        selection = WhiskrKitTheme.SelectionTheme(tintColor = selection, backgroundColor = Color(0xFF323244)), // raised background
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
}

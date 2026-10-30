package eu.whiskrkit.sample

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Everything the playground offers is described here as data, so the same
// structure can be mirrored on other platforms. See PLAYGROUND_SPEC.md.

/** A survey the playground can present, backed by one of the SDK's mocked surveys. */
internal class SurveyDescriptor(
    /** The mocked survey id passed to `WhiskrKit.present`. */
    val id: String,
    val name: String,
    val summary: String,
    val icon: ImageVector,
    val group: Group,
) {
    enum class Group(val title: String) {
        QuestionTypes("Question types"),
        PresentationStyles("Presentation styles"),
    }

    companion object {
        val all: List<SurveyDescriptor> = listOf(
            SurveyDescriptor(
                id = "onboarding-survey",
                name = "Scale rating",
                summary = "Sheet · 1–8 scale with a follow-up question",
                icon = Icons.Filled.Favorite,
                group = Group.QuestionTypes,
            ),
            SurveyDescriptor(
                id = "feature-feedback",
                name = "Symbol rating",
                summary = "Sheet · five stars",
                icon = Icons.Filled.Star,
                group = Group.QuestionTypes,
            ),
            SurveyDescriptor(
                id = "checkout-survey",
                name = "Thumbs rating",
                summary = "Sheet · thumbs up or down",
                icon = Icons.Filled.ThumbUp,
                group = Group.QuestionTypes,
            ),
            SurveyDescriptor(
                id = "ask-survey",
                name = "Text feedback",
                summary = "Sheet · free text, 200 characters",
                icon = Icons.Filled.Edit,
                group = Group.QuestionTypes,
            ),
            SurveyDescriptor(
                id = "choice-survey",
                name = "Multiple choice",
                summary = "Sheet · multi-select options",
                icon = Icons.Filled.CheckCircle,
                group = Group.QuestionTypes,
            ),
            SurveyDescriptor(
                id = "welcome-toast",
                name = "Banner",
                summary = "Thumbs rating at the bottom of the screen",
                icon = Icons.Filled.Notifications,
                group = Group.PresentationStyles,
            ),
            SurveyDescriptor(
                id = "super-survey",
                name = "Full-screen form",
                summary = "All question types in one form",
                icon = Icons.AutoMirrored.Filled.List,
                group = Group.PresentationStyles,
            ),
        )
    }
}

/**
 * A themeable color the playground exposes. One role can drive several
 * `WhiskrKitTheme` properties; [buildTheme] does that mapping.
 */
internal class ColorRole(
    /** Stable key, used for persistence and shared across platforms. */
    val id: String,
    val name: String,
    val description: String,
    /** The Material color the SDK uses while the role is not customized. */
    val systemColor: (ColorScheme) -> Color,
) {
    companion object {
        val Accent = ColorRole("accent", "Accent", "Primary button background, secondary button text") { it.primary }
        val OnAccent = ColorRole("onAccent", "On accent", "Primary button text") { it.onPrimary }
        val Background = ColorRole("background", "Background", "Sheet, form and banner background") {
            it.surfaceContainerLow
        }
        val Text = ColorRole("text", "Text", "Titles and body") { it.onSurface }
        val SecondaryText = ColorRole("secondaryText", "Secondary text", "Subtitles and subheadlines") {
            it.onSurfaceVariant
        }
        val Selection = ColorRole("selection", "Selection", "Selected multiple-choice options") { it.primary }

        val all: List<ColorRole> = listOf(Accent, OnAccent, Background, Text, SecondaryText, Selection)
    }
}

/**
 * A named set of custom colors (`#RRGGBB`), keyed by [ColorRole.id]. Roles left
 * out stay on their Material default.
 */
internal class ThemePreset(
    val id: String,
    val name: String,
    val colors: Map<String, String>,
) {
    companion object {
        /** No custom colors: the SDK's default look. Doubles as reset. */
        val System = ThemePreset("system", "System", emptyMap())

        /** Brand presets only set the accent roles, so surfaces and text keep adapting to dark mode. */
        val Ocean = ThemePreset(
            "ocean",
            "Ocean",
            mapOf("accent" to "#0066CC", "onAccent" to "#FFFFFF", "selection" to "#0066CC"),
        )
        val Sunset = ThemePreset(
            "sunset",
            "Sunset",
            mapOf("accent" to "#E8590C", "onAccent" to "#FFFFFF", "selection" to "#E8590C"),
        )

        /** Sets every role, which makes the theme fixed: it looks the same in light and dark mode. */
        val Midnight = ThemePreset(
            "midnight",
            "Midnight",
            mapOf(
                "accent" to "#F5C542",
                "onAccent" to "#1B1B2F",
                "background" to "#1B1B2F",
                "text" to "#F2F2F7",
                "secondaryText" to "#A5A5C0",
                "selection" to "#F5C542",
            ),
        )

        val all: List<ThemePreset> = listOf(System, Ocean, Sunset, Midnight)
    }
}

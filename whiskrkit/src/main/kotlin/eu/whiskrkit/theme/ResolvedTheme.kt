package eu.whiskrkit.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The fully-resolved theme used by all WhiskrKit composables. Resolution:
 * host-provided [WhiskrKitTheme] overrides win; unset fields fall back to the
 * ambient MaterialTheme at the host's position in the composition, so defaults
 * track dark mode and dynamic color automatically.
 */
@Immutable
internal data class ResolvedWhiskrTheme(
    val title: ResolvedTextTheme,
    val subtitle: ResolvedTextTheme,
    val headline: ResolvedTextTheme,
    val subheadline: ResolvedTextTheme,
    val body: ResolvedTextTheme,
    val primaryButton: ResolvedButtonAppearance,
    val secondaryButton: ResolvedButtonAppearance,
    val selectionTint: Color,
    val selectionBackground: Color,
    val sheetBackground: Color,
    val fullScreenBackground: Color,
    val bannerCornerRadius: Dp,
    val bannerBackground: Color,
    val bannerElevation: Dp,
)

@Immutable
internal data class ResolvedTextTheme(
    val textStyle: TextStyle,
    val color: Color,
)

internal sealed interface ResolvedButtonAppearance {
    @Immutable
    data class Variant(
        val backgroundColor: Color,
        val borderColor: Color?,
        val textColor: Color,
        val textStyle: TextStyle,
        val cornerRadius: Dp,
    ) : ResolvedButtonAppearance

    @Immutable
    data class Custom(
        val content: @Composable (text: String, enabled: Boolean, onClick: () -> Unit) -> Unit,
    ) : ResolvedButtonAppearance
}

/** Deliberately internal; hosts theme via `WhiskrKitHost(theme = ...)`. */
internal val LocalWhiskrKitTheme = staticCompositionLocalOf<ResolvedWhiskrTheme> {
    error("No WhiskrKitTheme provided — WhiskrKit composables must run inside WhiskrKitHost.")
}

@Composable
internal fun resolveWhiskrTheme(overrides: WhiskrKitTheme?): ResolvedWhiskrTheme {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    fun text(slot: WhiskrKitTheme.TextTheme?, defaultStyle: TextStyle, defaultColor: Color) =
        ResolvedTextTheme(
            textStyle = slot?.textStyle ?: defaultStyle,
            color = (slot?.color ?: Color.Unspecified).takeOrElse { defaultColor },
        )

    fun button(
        slot: WhiskrKitTheme.ButtonAppearance?,
        defaultBackground: Color,
        defaultText: Color,
    ): ResolvedButtonAppearance = when (slot) {
        is WhiskrKitTheme.ButtonAppearance.Custom -> ResolvedButtonAppearance.Custom(slot.content)
        is WhiskrKitTheme.ButtonAppearance.Variant -> ResolvedButtonAppearance.Variant(
            backgroundColor = slot.variant.backgroundColor.takeOrElse { defaultBackground },
            borderColor = slot.variant.borderColor,
            textColor = slot.variant.textColor.takeOrElse { defaultText },
            textStyle = slot.variant.textStyle ?: typography.labelLarge,
            cornerRadius = slot.variant.cornerRadius,
        )
        null -> ResolvedButtonAppearance.Variant(
            backgroundColor = defaultBackground,
            borderColor = null,
            textColor = defaultText,
            textStyle = typography.labelLarge,
            cornerRadius = 12.dp,
        )
    }

    return ResolvedWhiskrTheme(
        title = text(overrides?.title, typography.titleLarge, colors.onSurface),
        subtitle = text(overrides?.subtitle, typography.titleMedium, colors.onSurfaceVariant),
        headline = text(overrides?.headline, typography.titleMedium, colors.onSurface),
        subheadline = text(overrides?.subheadline, typography.bodyMedium, colors.onSurfaceVariant),
        body = text(overrides?.body, typography.bodyLarge, colors.onSurface),
        primaryButton = button(overrides?.button?.primary, colors.primary, colors.onPrimary),
        secondaryButton = button(overrides?.button?.secondary, Color.Transparent, colors.primary),
        selectionTint = (overrides?.selection?.tintColor ?: Color.Unspecified)
            .takeOrElse { colors.primary },
        selectionBackground = (overrides?.selection?.backgroundColor ?: Color.Unspecified)
            .takeOrElse { colors.surfaceContainerHigh },
        sheetBackground = (overrides?.container?.sheet?.backgroundColor ?: Color.Unspecified)
            .takeOrElse { colors.surfaceContainerLow },
        fullScreenBackground = (overrides?.container?.fullScreen?.backgroundColor ?: Color.Unspecified)
            .takeOrElse { colors.surface },
        bannerCornerRadius = overrides?.container?.banner?.cornerRadius ?: 12.dp,
        bannerBackground = (overrides?.container?.banner?.backgroundColor ?: Color.Unspecified)
            .takeOrElse { colors.surfaceContainerHigh },
        bannerElevation = overrides?.container?.banner?.elevation ?: 6.dp,
    )
}

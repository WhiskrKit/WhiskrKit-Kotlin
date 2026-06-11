package eu.whiskrkit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual customisation for WhiskrKit surveys.
 *
 * Every field is optional (decision #10, hybrid theming): anything you leave
 * unset resolves from the ambient [androidx.compose.material3.MaterialTheme]
 * at presentation time, so an unthemed WhiskrKit already matches a Material 3
 * app — including dark mode and dynamic color. Pass an instance to
 * [eu.whiskrkit.ui.WhiskrKitHost] to override selectively:
 *
 * ```kotlin
 * WhiskrKitHost(
 *     theme = WhiskrKitTheme(
 *         title = WhiskrKitTheme.TextTheme(color = MyBrand.ink),
 *         button = WhiskrKitTheme.ButtonTheme(
 *             primary = WhiskrKitTheme.ButtonAppearance.Variant(
 *                 WhiskrKitTheme.ButtonVariant(
 *                     backgroundColor = MyBrand.accent,
 *                     textColor = Color.White,
 *                 ),
 *             ),
 *         ),
 *     ),
 * ) { AppContent() }
 * ```
 */
@Immutable
public class WhiskrKitTheme(
    public val container: ContainerTheme? = null,
    public val selection: SelectionTheme? = null,
    public val button: ButtonTheme? = null,
    public val title: TextTheme? = null,
    public val subtitle: TextTheme? = null,
    public val headline: TextTheme? = null,
    public val subheadline: TextTheme? = null,
    public val body: TextTheme? = null,
) {

    /** Font and color for one text slot. Unset values fall back to Material defaults. */
    @Immutable
    public class TextTheme(
        public val textStyle: TextStyle? = null,
        public val color: Color = Color.Unspecified,
    )

    /** Tint and background for multiple-choice selection states. */
    @Immutable
    public class SelectionTheme(
        public val tintColor: Color = Color.Unspecified,
        public val backgroundColor: Color = Color.Unspecified,
    )

    @Immutable
    public class ButtonTheme(
        public val primary: ButtonAppearance? = null,
        public val secondary: ButtonAppearance? = null,
    )

    /**
     * Either a pre-baked [Variant] or a fully [Custom] composable slot — the
     * Compose counterpart of the iOS custom `ButtonStyle` escape hatch
     * (decision #12).
     */
    @Immutable
    public sealed class ButtonAppearance {
        public class Variant(public val variant: ButtonVariant) : ButtonAppearance()

        public class Custom(
            public val content: @Composable (text: String, enabled: Boolean, onClick: () -> Unit) -> Unit,
        ) : ButtonAppearance()
    }

    /**
     * Note for iOS parity readers: the iOS `ButtonVariant` also accepted a
     * `size` parameter that was never stored; it is intentionally absent here
     * (decision P2).
     */
    @Immutable
    public class ButtonVariant(
        public val backgroundColor: Color = Color.Unspecified,
        public val borderColor: Color? = null,
        public val textColor: Color = Color.Unspecified,
        public val textStyle: TextStyle? = null,
        public val cornerRadius: Dp = 12.dp,
    )

    @Immutable
    public class ContainerTheme(
        public val sheet: OverlayTheme? = null,
        public val fullScreen: OverlayTheme? = null,
        public val banner: BannerTheme? = null,
    )

    /** Background for sheet and full-screen survey containers. */
    @Immutable
    public class OverlayTheme(
        public val backgroundColor: Color = Color.Unspecified,
    )

    /** Styling for the banner container ("toast" in iOS terminology). */
    @Immutable
    public class BannerTheme(
        public val cornerRadius: Dp = 12.dp,
        public val backgroundColor: Color = Color.Unspecified,
        public val elevation: Dp = 6.dp,
    )
}

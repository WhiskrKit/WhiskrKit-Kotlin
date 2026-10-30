package eu.whiskrkit.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import eu.whiskrkit.ui.WhiskrKitHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as SampleApplication).themeSettings

        setContent {
            var appearance by rememberSaveable { mutableStateOf(PreviewAppearance.System) }
            val darkTheme = when (appearance) {
                PreviewAppearance.System -> isSystemInDarkTheme()
                PreviewAppearance.Light -> false
                PreviewAppearance.Dark -> true
            }

            // Keep the system bar icons legible in the forced appearance.
            LaunchedEffect(darkTheme) {
                val style = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            // The baseline Material scheme (no dynamic color), so "System"
            // looks the same on every device.
            MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
                val theme = remember(settings.customColors) { buildTheme(settings.customColors) }
                WhiskrKitHost(theme = theme) {
                    PlaygroundScreen(
                        settings = settings,
                        appearance = appearance,
                        onAppearanceChange = { appearance = it },
                    )
                }
            }
        }
    }

    private companion object {
        const val TRANSPARENT = android.graphics.Color.TRANSPARENT
    }
}

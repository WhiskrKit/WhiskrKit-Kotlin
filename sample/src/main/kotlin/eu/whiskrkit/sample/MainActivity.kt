package eu.whiskrkit.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.whiskrkit.WhiskrKit
import eu.whiskrkit.theme.WhiskrKitTheme
import eu.whiskrkit.ui.WhiskrKitHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SampleApp()
        }
    }
}

/** A loud custom theme to make override behaviour obvious next to the default. */
private val customTheme = WhiskrKitTheme(
    title = WhiskrKitTheme.TextTheme(color = Color(0xFF6750A4)),
    selection = WhiskrKitTheme.SelectionTheme(tintColor = Color(0xFF6750A4)),
    button = WhiskrKitTheme.ButtonTheme(
        primary = WhiskrKitTheme.ButtonAppearance.Variant(
            WhiskrKitTheme.ButtonVariant(
                backgroundColor = Color(0xFF6750A4),
                textColor = Color.White,
                cornerRadius = 24.dp,
            ),
        ),
    ),
    container = WhiskrKitTheme.ContainerTheme(
        banner = WhiskrKitTheme.BannerTheme(cornerRadius = 20.dp),
    ),
)

@Composable
private fun SampleApp() {
    var useCustomTheme by rememberSaveable { mutableStateOf(false) }
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        WhiskrKitHost(theme = if (useCustomTheme) customTheme else null) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "WhiskrKit Sample",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Custom theme",
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = useCustomTheme,
                            onCheckedChange = { useCustomTheme = it },
                        )
                    }

                    SectionHeader("Banners")
                    TriggerButton("Thumbs banner", "welcome-toast")
                    TriggerButton("Star-rating banner", "quick-feedback")
                    TriggerButton("Plain banner", "simple-toast")
                    TriggerButton("Banner → follow-up sheet", "feedback-toast")

                    SectionHeader("Sheets")
                    TriggerButton("Scale + follow-up", "onboarding-survey")
                    TriggerButton("NPS 0–10", "nps-survey")
                    TriggerButton("Free text", "text-feedback")
                    TriggerButton("Single choice", "choice-survey")
                    TriggerButton("Multi choice", "multi-choice-survey")

                    SectionHeader("Full-screen")
                    TriggerButton("Multi-question form", "full-survey")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun TriggerButton(label: String, surveyId: String) {
    Button(
        onClick = { WhiskrKit.present(surveyId) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label)
    }
}

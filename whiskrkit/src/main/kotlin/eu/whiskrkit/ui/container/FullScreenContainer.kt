package eu.whiskrkit.ui.container

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eu.whiskrkit.R
import eu.whiskrkit.WhiskrKit
import eu.whiskrkit.core.model.FullScreenFormTemplate
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.ui.WhiskrIcons
import eu.whiskrkit.internal.WhiskrLog
import eu.whiskrkit.theme.LocalWhiskrKitTheme
import eu.whiskrkit.theme.WhiskrButton
import eu.whiskrkit.theme.WhiskrButtonKind
import eu.whiskrkit.ui.question.QuestionView
import kotlinx.coroutines.launch

/**
 * Full-screen presentation via a Compose Dialog with platform width disabled —
 * stays inside the host's composition so theme and CompositionLocals
 * propagate, and back-press dismisses without submitting.
 */
@Composable
internal fun FullScreenContainer(template: FullScreenFormTemplate, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        FullScreenContent(
            template = template,
            onClose = onDismiss,
            onSubmitted = onDismiss,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullScreenContent(
    template: FullScreenFormTemplate,
    onClose: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalWhiskrKitTheme.current
    var response by rememberSaveable(stateSaver = SurveyResponseSaver) {
        mutableStateOf(SurveyResponse())
    }
    var submitAttempted by rememberSaveable { mutableStateOf(false) }

    val missingQuestionIds = template.surveys
        .filter { it.isRequired && it.id !in response.results }
        .map { it.id }

    fun submit() {
        if (missingQuestionIds.isEmpty()) {
            WhiskrLog.i(WhiskrLog.UI, "User submitted fullscreen survey.")
            val submitted = response
            WhiskrKit.scope.launch {
                WhiskrKit.submitSurveyResponse(template.id, submitted)
            }
            onSubmitted()
        } else {
            submitAttempted = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = theme.fullScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = template.title ?: stringResource(R.string.whiskrkit_survey),
                            style = theme.title.textStyle,
                            color = theme.title.color,
                        )
                        if (template.subtitle != null) {
                            Text(
                                text = template.subtitle,
                                style = theme.subheadline.textStyle,
                                color = theme.subheadline.color,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = WhiskrIcons.Close,
                            contentDescription = stringResource(R.string.whiskrkit_close),
                            tint = theme.body.color,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.fullScreenBackground,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            if (template.description != null) {
                HorizontalDivider()
                Text(
                    text = template.description,
                    style = theme.body.textStyle,
                    color = theme.body.color,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                HorizontalDivider()
            }

            for (question in template.surveys) {
                QuestionView(
                    template = question,
                    answer = response.results[question.id],
                    onAnswer = { answer ->
                        response = response.withAnswer(question.id, answer)
                    },
                    showError = submitAttempted && question.id in missingQuestionIds,
                )
                HorizontalDivider()
            }

            Spacer(modifier = Modifier.height(24.dp))
            WhiskrButton(
                text = stringResource(R.string.whiskrkit_submit),
                onClick = ::submit,
                kind = WhiskrButtonKind.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

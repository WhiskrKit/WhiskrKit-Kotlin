package eu.whiskrkit.ui.container

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.whiskrkit.R
import eu.whiskrkit.WhiskrKit
import eu.whiskrkit.core.model.SheetTemplate
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.model.TextSurveyTemplate
import eu.whiskrkit.ui.WhiskrIcons
import eu.whiskrkit.internal.WhiskrLog
import eu.whiskrkit.theme.LocalWhiskrKitTheme
import eu.whiskrkit.theme.WhiskrButton
import eu.whiskrkit.theme.WhiskrButtonKind
import eu.whiskrkit.ui.question.QuestionView
import kotlinx.coroutines.launch

/**
 * Sheet presentation via Material3 ModalBottomSheet. Content height wrapping
 * is native; insets are handled in the content so the sheet grows with the
 * keyboard when the follow-up text field gets focus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetContainer(template: SheetTemplate, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val animationScope = androidx.compose.runtime.rememberCoroutineScope()
    val theme = LocalWhiskrKitTheme.current

    fun animateDismiss() {
        animationScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = theme.sheetBackground,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        SheetContent(
            template = template,
            onClose = ::animateDismiss,
            onSubmitted = ::animateDismiss,
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding(),
        )
    }
}

@Composable
internal fun SheetContent(
    template: SheetTemplate,
    onClose: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalWhiskrKitTheme.current
    var response by rememberSaveable(stateSaver = SurveyResponseSaver) {
        mutableStateOf(SurveyResponse())
    }
    var submitAttempted by rememberSaveable { mutableStateOf(false) }

    // The sheet's main question must be answered to submit, regardless of
    // isRequired.
    val canSubmit = template.survey.id in response.results

    fun submit() {
        if (canSubmit) {
            WhiskrLog.i(WhiskrLog.UI, "User submitted sheet survey.")
            val submitted = response
            WhiskrKit.scope.launch {
                WhiskrKit.submitSurveyResponse(template.id, submitted)
            }
            onSubmitted()
        } else {
            submitAttempted = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                if (template.title != null) {
                    Text(
                        text = template.title,
                        style = theme.title.textStyle,
                        color = theme.title.color,
                    )
                }
                if (template.description != null) {
                    Text(
                        text = template.description,
                        style = theme.subtitle.textStyle,
                        color = theme.subtitle.color,
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = WhiskrIcons.Close,
                    contentDescription = stringResource(R.string.whiskrkit_close),
                    tint = theme.body.color,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        QuestionView(
            template = template.survey,
            answer = response.results[template.survey.id],
            onAnswer = { answer ->
                response = response.withAnswer(template.survey.id, answer)
            },
            showError = submitAttempted && template.survey.id !in response.results,
        )

        if (template.followUpQuestion != null && response.results.isNotEmpty()) {
            val followUpTemplate = TextSurveyTemplate(
                id = "${template.id}-followUp",
                title = template.followUpQuestion,
                isRequired = false,
            )
            QuestionView(
                template = followUpTemplate,
                answer = response.results[followUpTemplate.id],
                onAnswer = { answer ->
                    response = response.withAnswer(followUpTemplate.id, answer)
                },
                showError = false,
            )
        }

        WhiskrButton(
            text = stringResource(R.string.whiskrkit_submit),
            onClick = ::submit,
            kind = WhiskrButtonKind.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

internal fun SurveyResponse.withAnswer(
    questionId: String,
    answer: eu.whiskrkit.core.model.SurveyAnswer?,
): SurveyResponse = copy(
    results = if (answer == null) results - questionId else results + (questionId to answer),
)

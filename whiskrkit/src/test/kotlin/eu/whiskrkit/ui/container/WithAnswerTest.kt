package eu.whiskrkit.ui.container

import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SurveyResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The containers store every answer through [withAnswer]. Scale ratings pass
 * `null` when the selected score is tapped again; that must remove the answer,
 * not keep the old score for submission.
 */
class WithAnswerTest {

    @Test
    fun `selecting a score stores it`() {
        val response = SurveyResponse().withAnswer("q1", SurveyAnswer.NpsRating(8))
        assertEquals(SurveyAnswer.NpsRating(8), response.results["q1"])
    }

    @Test
    fun `deselecting a score removes the answer and keeps the others`() {
        val response = SurveyResponse(mapOf("q2" to SurveyAnswer.Text("Keep me")))
            .withAnswer("q1", SurveyAnswer.NpsRating(8))
            .withAnswer("q1", null)
        assertNull(response.results["q1"])
        assertEquals(SurveyAnswer.Text("Keep me"), response.results["q2"])
    }
}

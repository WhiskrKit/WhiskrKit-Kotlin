package eu.whiskrkit.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {

    @Test
    fun `black on white is 21 to 1 and identical colors are 1 to 1`() {
        assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.01f)
        assertEquals(21f, contrastRatio(Color.White, Color.Black), 0.01f)
        assertEquals(1f, contrastRatio(Color.Red, Color.Red), 0.001f)
    }

    @Test
    fun `matches the WCAG reference value for mid grey`() {
        // #777777 on white is the classic 4.48:1.
        assertEquals(4.48f, contrastRatio(Color(0xFF777777), Color.White), 0.01f)
    }

    @Test
    fun `translucent foreground is blended over the background`() {
        val halfBlack = Color.Black.copy(alpha = 0.5f)
        val blended = contrastRatio(halfBlack, Color.White)
        assertTrue(blended < 21f && blended > 3f)
        assertEquals(contrastRatio(Color(0xFF808080), Color.White), blended, 0.1f)
    }

    @Test
    fun `translucent background is blended over the backdrop`() {
        val clear = Color.White.copy(alpha = 0f)
        assertEquals(21f, contrastRatio(Color.Black, clear, backdrop = Color.White), 0.01f)
        assertEquals(21f, contrastRatio(Color.White, clear, backdrop = Color.Black), 0.01f)
    }

    @Test
    fun `a readable preferred color is kept`() {
        val grey = Color(0xFF49454F) // Material onSurfaceVariant on a light surface
        assertEquals(grey, readableColor(grey, Color(0xFFF7F2FA)))
    }

    @Test
    fun `a low contrast color is blended toward black on light and white on dark`() {
        val faint = Color(0xFFBBBBBB)
        val onLight = readableColor(faint, Color.White)
        assertTrue(contrastRatio(onLight, Color.White) >= MIN_TEXT_CONTRAST)
        assertTrue(onLight != Color.Black && onLight.red < faint.red)

        val dim = Color(0xFF3A3A55)
        val onDark = readableColor(dim, Color(0xFF1B1B2F))
        assertTrue(contrastRatio(onDark, Color(0xFF1B1B2F)) >= MIN_TEXT_CONTRAST)
        assertTrue(onDark != Color.White && onDark.blue > dim.blue)
    }

    @Test
    fun `the blend stops at the first step that reaches 4_5 and keeps the hue`() {
        val tan = Color(0xFFD7B899)
        val result = readableColor(tan, Color(0xFFFFF3E0))
        assertTrue(contrastRatio(result, Color(0xFFFFF3E0)) >= MIN_TEXT_CONTRAST)
        assertTrue(result.red > result.blue) // still warm
        // Smallest change: not pure black.
        assertTrue(result != Color.Black)
    }

    @Test
    fun `on a mid tone where only the extremes work the result is black or white`() {
        // Luminance near 0.18 is the hardest background: only pure black reaches 4.5:1.
        val mid = Color(0xFF757575)
        val result = readableColor(mid, mid)
        assertTrue(contrastRatio(result, mid) >= MIN_TEXT_CONTRAST)
        assertTrue(result.red < 0.05f || result.red > 0.95f)
    }

    @Test
    fun `threshold is inclusive at 4_5`() {
        // #767676 on white is 4.54:1: kept. #777777 is 4.48:1: replaced.
        assertEquals(Color(0xFF767676), readableColor(Color(0xFF767676), Color.White))
        val softened = readableColor(Color(0xFF777777), Color.White)
        assertTrue(contrastRatio(softened, Color.White) >= MIN_TEXT_CONTRAST)
        assertTrue(softened.red > 0.3f) // a slight darkening, nowhere near black
    }
}

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
    fun `a low contrast color falls back to black on light and white on dark`() {
        assertEquals(Color.Black, readableColor(Color(0xFFBBBBBB), Color.White))
        assertEquals(Color.White, readableColor(Color(0xFF3A3A55), Color(0xFF1B1B2F)))
    }

    @Test
    fun `fallback picks whichever of black and white contrasts more on a mid tone`() {
        val orange = Color(0xFFE8590C)
        val pick = readableColor(Color(0xFFE9600F), orange)
        val other = if (pick == Color.Black) Color.White else Color.Black
        assertTrue(contrastRatio(pick, orange) >= contrastRatio(other, orange))
    }

    @Test
    fun `threshold is inclusive at 4_5`() {
        // #767676 on white is 4.54:1: kept. #777777 is 4.48:1: replaced.
        assertEquals(Color(0xFF767676), readableColor(Color(0xFF767676), Color.White))
        assertEquals(Color.Black, readableColor(Color(0xFF777777), Color.White))
    }
}

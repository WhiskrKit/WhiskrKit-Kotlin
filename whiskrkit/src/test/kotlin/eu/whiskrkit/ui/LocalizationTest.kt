package eu.whiskrkit.ui

import eu.whiskrkit.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalizationTest {

    private val context get() = RuntimeEnvironment.getApplication()

    private fun required(language: String): String =
        context.withLanguage(language).getString(R.string.whiskrkit_required)

    // MARK: - Survey language

    @Test
    fun `survey language switches WhiskrKit's own strings`() {
        assertEquals("Verplicht", required("nl"))
        assertEquals("Erforderlich", required("de"))
        assertEquals("必須", required("ja"))
    }

    @Test
    fun `two-letter codes from the backend find the regional translations`() {
        assertEquals("Obrigatório", required("pt"))
        assertEquals("Obrigatório", required("pt-BR"))
        assertEquals("必填", required("zh"))
        assertEquals("必填", required("zh-CN"))
    }

    @Test
    fun `a language WhiskrKit doesn't ship falls back to English`() {
        assertEquals("Required", required("sv"))
    }

    @Test
    fun `star plurals follow the language`() {
        fun stars(language: String, count: Int) =
            context.withLanguage(language).resources.getQuantityString(R.plurals.whiskrkit_a11y_star, count, count)

        assertEquals("1 star", stars("en", 1))
        assertEquals("5 stars", stars("en", 5))
        assertEquals("1 ster", stars("nl", 1))
        assertEquals("5 sterren", stars("nl", 5))
        assertEquals("1 étoile", stars("fr", 1))
        assertEquals("星5個", stars("ja", 5))
    }

    // MARK: - Resources

    /** Every string must exist in every shipped language, or that language silently shows English. */
    @Test
    fun `every language has every string`() {
        val res = File("src/main/res")
        val english = keys(File(res, "values/strings.xml"))
        val languages = res.listFiles { file -> file.name.startsWith("values-") && File(file, "strings.xml").exists() }
            .orEmpty()
        assertTrue("No translations found in $res", languages.size >= 9)

        for (folder in languages) {
            assertEquals("Keys in ${folder.name}", english, keys(File(folder, "strings.xml")))
        }
    }

    private fun keys(file: File): Set<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = document.documentElement
        val strings = root.getElementsByTagName("string").elements()
            .filter { it.getAttribute("translatable") != "false" }
            .map { "string/${it.getAttribute("name")}" }
        val plurals = root.getElementsByTagName("plurals").elements().map { plural ->
            val quantities = plural.getElementsByTagName("item").elements().map { it.getAttribute("quantity") }
            assertTrue("${file.parentFile.name}: ${plural.getAttribute("name")} has no 'other'", "other" in quantities)
            "plurals/${plural.getAttribute("name")}"
        }
        return (strings + plurals).toSet()
    }

    private fun org.w3c.dom.NodeList.elements(): List<Element> = (0 until length).map { item(it) as Element }
}

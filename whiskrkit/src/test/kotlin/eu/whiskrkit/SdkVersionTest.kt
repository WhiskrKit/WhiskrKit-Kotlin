package eu.whiskrkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SdkVersionTest {

    /**
     * `sdkVersion` in whiskrkit/build.gradle.kts is published and sent with every
     * request. Releases are named after the newest CHANGELOG.md version, so the two
     * must match. Fails when a release bumps one but not the other.
     */
    @Test
    fun `the SDK version matches the newest version in CHANGELOG md`() {
        // Gradle runs unit tests in the module directory.
        val changelog = File("../CHANGELOG.md").takeIf { it.exists() } ?: File("CHANGELOG.md")
        val newest = changelog.readLines()
            .firstNotNullOfOrNull { Regex("""## (\d+\.\d+\.\d+)( .*)?""").matchEntire(it)?.groupValues?.get(1) }
        assertNotNull("CHANGELOG.md has no version heading", newest)
        assertEquals(newest, BuildConfig.SDK_VERSION)
    }

    @Test
    fun `the SDK version is a plain semantic version`() {
        assertTrue(Regex("""\d+\.\d+\.\d+""").matches(BuildConfig.SDK_VERSION))
    }
}

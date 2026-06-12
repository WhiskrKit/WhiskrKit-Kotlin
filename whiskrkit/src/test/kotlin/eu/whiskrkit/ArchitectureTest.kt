package eu.whiskrkit

import com.lemonappdev.konsist.api.Konsist
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Enforces the SDK's layering: the core packages stay Compose-free so a
 * future core/compose module split is a mechanical move, not a refactor.
 */
class ArchitectureTest {

    @Test
    fun `core and internal packages do not depend on Compose`() {
        val offending = Konsist
            .scopeFromProduction("whiskrkit")
            .files
            .filter { file ->
                val pkg = file.packagee?.name.orEmpty()
                pkg.startsWith("eu.whiskrkit.core") || pkg == "eu.whiskrkit.internal"
            }
            .flatMap { file ->
                file.imports
                    .filter { it.name.startsWith("androidx.compose") }
                    .map { "${file.name}: ${it.name}" }
            }

        assertTrue(
            "Core/internal files must not import Compose:\n${offending.joinToString("\n")}",
            offending.isEmpty(),
        )
    }

    @Test
    fun `singleton entry point does not depend on Compose`() {
        val offending = Konsist
            .scopeFromProduction("whiskrkit")
            .files
            .filter { it.name == "WhiskrKit" && it.packagee?.name == "eu.whiskrkit" }
            .flatMap { file ->
                file.imports
                    .filter { it.name.startsWith("androidx.compose") }
                    .map { it.name }
            }

        assertTrue(
            "WhiskrKit.kt must not import Compose: $offending",
            offending.isEmpty(),
        )
    }
}

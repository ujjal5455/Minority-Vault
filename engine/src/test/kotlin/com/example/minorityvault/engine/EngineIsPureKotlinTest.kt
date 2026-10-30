package com.example.minorityvault.engine

import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test
import java.io.File

/** CLAUDE.md §3 rule 1: :engine has zero Android dependencies. */
class EngineIsPureKotlinTest {
    private val forbiddenImports = listOf("android.", "androidx.", "com.google.android.")

    @Test
    fun `main sources import nothing from Android`() {
        val offenders = File("src/main")
            .walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "java") }
            .flatMap { file ->
                file.readLines()
                    .map { it.trim() }
                    .filter { line -> forbiddenImports.any { line.startsWith("import $it") } }
                    .map { "${file.path}: $it" }
            }
            .toList()

        offenders.shouldBeEmpty()
    }
}

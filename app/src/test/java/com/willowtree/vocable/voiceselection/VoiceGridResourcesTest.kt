package com.willowtree.vocable.voiceselection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pins the Change Voice picker's grid shape per screen-size breakpoint (#644).
 *
 * `VoiceSelectionScreen` reads `voice_columns`/`voice_rows` via `integerResource`, so the layout is
 * only as correct as the resource matrix behind it — and the classic failure is adding a value to
 * `values/` alone and letting every other breakpoint silently fall back to it. This repo has no
 * Robolectric (checked `libs.versions.toml`), so the matrix is asserted by parsing the XML off disk
 * rather than through a real `Resources` instance.
 *
 * The expected values mirror iOS's size-class switch in
 * `VoicePickerViewController.updateLayoutForCurrentTraitCollection()`: `fixedCount(1)` column for
 * `hCompact_vRegular` (phone portrait) and `fixedCount(2)` for every other size class.
 */
class VoiceGridResourcesTest {

    private data class GridShape(val columns: Int, val rows: Int)

    private val expectedShapes = mapOf(
        // iOS hCompact_vRegular — the only single-column case.
        "values" to GridShape(columns = 1, rows = 5),
        "values-sw400dp" to GridShape(columns = 1, rows = 5),
        // iOS hCompact_vCompact.
        "values-land" to GridShape(columns = 2, rows = 3),
        "values-sw400dp-land" to GridShape(columns = 2, rows = 3),
        // iOS hRegular_vRegular / hRegular_vCompact. Portrait carries more rows than landscape
        // because rows stretch to fill the page — see the comment in values-sw600dp/integers.xml.
        "values-sw600dp" to GridShape(columns = 2, rows = 7),
        "values-sw600dp-land" to GridShape(columns = 2, rows = 4)
    )

    private val resDir: File by lazy {
        // The Gradle test working directory differs between an IDE run and a CLI run, so walk up
        // from the working directory looking for the module's res dir instead of assuming either.
        val candidates = generateSequence(File("").absoluteFile) { it.parentFile }
            .flatMap { sequenceOf(File(it, "src/main/res"), File(it, "app/src/main/res")) }
        candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main/res from ${File("").absolutePath}")
    }

    private fun integersIn(valuesDir: String): Map<String, Int> {
        val file = File(resDir, "$valuesDir/integers.xml")
        assertTrue("Missing $valuesDir/integers.xml", file.isFile)

        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("integer")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            val name = node.attributes.getNamedItem("name").nodeValue
            name to node.textContent.trim().toInt()
        }
    }

    @Test
    fun `voice grid shape is defined in every breakpoint dir`() {
        expectedShapes.forEach { (valuesDir, _) ->
            val integers = integersIn(valuesDir)

            assertNotNull(
                "$valuesDir/integers.xml must define voice_columns rather than falling back",
                integers["voice_columns"]
            )
            assertNotNull(
                "$valuesDir/integers.xml must define voice_rows rather than falling back",
                integers["voice_rows"]
            )
        }
    }

    @Test
    fun `voice grid shape matches the iOS size-class matrix`() {
        expectedShapes.forEach { (valuesDir, expected) ->
            val integers = integersIn(valuesDir)

            assertEquals(
                "voice_columns in $valuesDir",
                expected.columns,
                integers["voice_columns"]
            )
            assertEquals(
                "voice_rows in $valuesDir",
                expected.rows,
                integers["voice_rows"]
            )
        }
    }

    @Test
    fun `only phone portrait breakpoints are single-column`() {
        val singleColumnDirs = expectedShapes.keys
            .filter { integersIn(it)["voice_columns"] == 1 }
            .toSet()

        assertEquals(
            "iOS uses fixedCount(1) only for hCompact_vRegular; every other size class is 2 columns",
            setOf("values", "values-sw400dp"),
            singleColumnDirs
        )
    }

    @Test
    fun `no other values dir overrides the voice grid`() {
        // Locale-qualified dirs such as values-de exist only to widen the keyboard for long German
        // keycaps. If one of them ever picked up a voice_* override, the picker's shape would change
        // by locale, which is not intended and would not show up in the matrix above.
        val unexpected = resDir.listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values") }
            .filter { it.name !in expectedShapes.keys }
            .filter { dir ->
                val integers = File(dir, "integers.xml")
                integers.isFile && integersIn(dir.name).keys.any { it.startsWith("voice_") }
            }
            .map { it.name }

        assertEquals(emptyList<String>(), unexpected)
    }
}

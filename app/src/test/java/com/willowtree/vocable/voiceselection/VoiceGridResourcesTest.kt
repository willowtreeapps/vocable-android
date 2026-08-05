package com.willowtree.vocable.voiceselection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pins the Change Voice picker's grid shape per screen-size breakpoint (#644, #667).
 *
 * `VoiceSelectionScreen` reads `voice_columns` via `integerResource` and `voice_row_min_height` via
 * `dimensionResource`, so the layout is only as correct as the resource matrix behind it — and the
 * classic failure is adding a value to `values/` alone and letting every other breakpoint silently
 * fall back to it. This repo has no Robolectric (checked `libs.versions.toml`), so the matrix is
 * asserted by parsing the XML off disk rather than through a real `Resources` instance.
 *
 * The expected column counts mirror iOS's size-class switch in
 * `VoicePickerViewController.updateLayoutForCurrentTraitCollection()`: `fixedCount(1)` column for
 * `hCompact_vRegular` (phone portrait) and `fixedCount(2)` for every other size class.
 *
 * Row *counts* are deliberately absent from the resources — see [VoiceRowCountTest].
 */
class VoiceGridResourcesTest {

    private val expectedColumns = mapOf(
        // iOS hCompact_vRegular — the only single-column case.
        "values" to 1,
        "values-sw400dp" to 1,
        // iOS hCompact_vCompact.
        "values-land" to 2,
        "values-sw400dp-land" to 2,
        // iOS hRegular_vRegular / hRegular_vCompact.
        "values-sw600dp" to 2,
        "values-sw600dp-land" to 2
    )

    /**
     * Only the four dirs that carry a `dimens.xml`; the two `sw400dp` dirs intentionally inherit
     * their row height from `values` / `values-land`.
     */
    private val expectedRowMinHeights = mapOf(
        "values" to "60dp",
        "values-land" to "48dp",
        "values-sw600dp" to "80dp",
        "values-sw600dp-land" to "64dp"
    )

    private val resDir: File by lazy {
        // The Gradle test working directory differs between an IDE run and a CLI run, so walk up
        // from the working directory looking for the module's res dir instead of assuming either.
        val candidates = generateSequence(File("").absoluteFile) { it.parentFile }
            .flatMap { sequenceOf(File(it, "src/main/res"), File(it, "app/src/main/res")) }
        candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main/res from ${File("").absolutePath}")
    }

    private fun valuesIn(valuesDir: String, fileName: String, tag: String): Map<String, String> {
        val file = File(resDir, "$valuesDir/$fileName")
        assertTrue("Missing $valuesDir/$fileName", file.isFile)

        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName(tag)
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            val name = node.attributes.getNamedItem("name").nodeValue
            name to node.textContent.trim()
        }
    }

    private fun integersIn(valuesDir: String): Map<String, Int> =
        valuesIn(valuesDir, "integers.xml", "integer").mapValues { it.value.toInt() }

    private fun dimensIn(valuesDir: String): Map<String, String> =
        valuesIn(valuesDir, "dimens.xml", "dimen")

    @Test
    fun `voice column count is defined in every breakpoint dir`() {
        expectedColumns.forEach { (valuesDir, _) ->
            assertNotNull(
                "$valuesDir/integers.xml must define voice_columns rather than falling back",
                integersIn(valuesDir)["voice_columns"]
            )
        }
    }

    @Test
    fun `voice column count matches the iOS size-class matrix`() {
        expectedColumns.forEach { (valuesDir, expected) ->
            assertEquals(
                "voice_columns in $valuesDir",
                expected,
                integersIn(valuesDir)["voice_columns"]
            )
        }
    }

    @Test
    fun `only phone portrait breakpoints are single-column`() {
        val singleColumnDirs = expectedColumns.keys
            .filter { integersIn(it)["voice_columns"] == 1 }
            .toSet()

        assertEquals(
            "iOS uses fixedCount(1) only for hCompact_vRegular; every other size class is 2 columns",
            setOf("values", "values-sw400dp"),
            singleColumnDirs
        )
    }

    @Test
    fun `row height is a per-breakpoint minimum, not a row count`() {
        // The row count is derived from the page height against voice_row_min_height (#667). A
        // `voice_rows` integer coming back would mean a fixed count had been reintroduced, which is
        // what left a band of dead space below a full page: `sw###dp` bounds width, not height, so
        // no single count fits every device in a bucket.
        expectedColumns.forEach { (valuesDir, _) ->
            assertNull(
                "$valuesDir/integers.xml must not define voice_rows — the count is derived",
                integersIn(valuesDir)["voice_rows"]
            )
        }

        expectedRowMinHeights.forEach { (valuesDir, expected) ->
            assertEquals(
                "voice_row_min_height in $valuesDir",
                expected,
                dimensIn(valuesDir)["voice_row_min_height"]
            )
        }
    }

    @Test
    fun `no other values dir overrides the voice grid`() {
        // Locale-qualified dirs such as values-de exist only to widen the keyboard for long German
        // keycaps. If one of them ever picked up a voice_columns or voice_row_min_height override,
        // the picker's shape — and, since the row count is derived from that height, how many voices
        // a page holds — would change by locale. Not intended, and invisible in the matrix above.
        val unexpected = resDir.listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values") }
            .filter { it.name !in expectedColumns.keys }
            .filter { dir ->
                val definesColumns = File(dir, "integers.xml").isFile &&
                    integersIn(dir.name).keys.any { it.startsWith("voice_") }
                val definesRowHeight = File(dir, "dimens.xml").isFile &&
                    dimensIn(dir.name).containsKey("voice_row_min_height")
                definesColumns || definesRowHeight
            }
            .map { it.name }

        assertEquals(emptyList<String>(), unexpected)
    }
}

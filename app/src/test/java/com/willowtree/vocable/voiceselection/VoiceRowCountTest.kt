package com.willowtree.vocable.voiceselection

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.willowtree.vocable.ui.voiceselection.voiceRowCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the Change Voice picker's derived row count (#667).
 *
 * #644 read the count from a `voice_rows` resource per breakpoint dir, which left a band of dead
 * space below a *full* page: `sw###dp` qualifiers constrain width and never height, so one count per
 * width bucket cannot fit both a 640dp-tall and an 851dp-tall phone. The count is now derived from
 * the page's own height against `voice_row_min_height`, and the two properties that has to satisfy —
 * every row at least the design height, and no room left for another row — are asserted here as a
 * sweep rather than only at the handful of device sizes that happened to get checked on an emulator.
 *
 * [VoiceGridResourcesTest] pins the resources these geometries are taken from.
 */
class VoiceRowCountTest {

    /**
     * One entry per `dimens.xml` dir, with the chrome that sits outside the grid:
     * `2 × voice_screen_margin + voice_close_button_size + 2 × voice_section_spacing +
     * voice_paging_button_size`, matching what `VoiceSelectionScreen` subtracts from the screen
     * height. The two `sw400dp` dirs inherit `values` / `values-land`, so they are not separate
     * cases.
     */
    private data class Breakpoint(
        val name: String,
        val rowMinHeight: Dp,
        val rowSpacing: Dp,
        val chrome: Dp
    )

    private val breakpoints = listOf(
        Breakpoint("values", rowMinHeight = 60.dp, rowSpacing = 12.dp, chrome = 216.dp),
        Breakpoint("values-land", rowMinHeight = 48.dp, rowSpacing = 8.dp, chrome = 136.dp),
        Breakpoint("values-sw600dp", rowMinHeight = 80.dp, rowSpacing = 16.dp, chrome = 256.dp),
        Breakpoint("values-sw600dp-land", rowMinHeight = 64.dp, rowSpacing = 12.dp, chrome = 256.dp)
    )

    private fun Breakpoint.rowsOnScreen(screenHeight: Dp): Int =
        voiceRowCount(screenHeight - chrome, rowMinHeight, rowSpacing)

    /** Height each of [rows] rows actually gets once they stretch to fill the grid. */
    private fun Breakpoint.renderedRowHeight(screenHeight: Dp, rows: Int): Dp =
        (screenHeight - chrome - rowSpacing * (rows - 1)) / rows

    @Test
    fun `reference devices fill their page`() {
        // screen height, breakpoint, expected rows — the four dimens dirs across the device shapes
        // this was verified on. The point of each case is that it differs: a fixed count per width
        // bucket could not serve the three `values` heights below at once.
        val cases = listOf(
            Triple(851.dp, "values", 8), // phone portrait, 393x851dp
            Triple(800.dp, "values", 8), // phone portrait, 360x800dp
            Triple(640.dp, "values", 6), // short phone portrait — where a tuned-up fixed count clips
            Triple(915.dp, "values", 9), // 412x915dp, inherited by values-sw400dp
            Triple(393.dp, "values-land", 4), // phone landscape
            Triple(1280.dp, "values-sw600dp", 10), // tablet portrait, 800x1280dp
            Triple(800.dp, "values-sw600dp-land", 7) // tablet landscape, 1280x800dp
        )

        cases.forEach { (screenHeight, dirName, expectedRows) ->
            val breakpoint = breakpoints.first { it.name == dirName }

            assertEquals(
                "rows at $screenHeight in $dirName",
                expectedRows,
                breakpoint.rowsOnScreen(screenHeight)
            )
        }
    }

    @Test
    fun `a row is never shorter than the design height`() {
        // Gaze targets shrinking below the height design signed off on is the failure that matters
        // most here: the whole point of deriving the count is that rows stay chip-height.
        breakpoints.forEach { breakpoint ->
            (240..1600).forEach { height ->
                val screenHeight = height.dp
                // Below this the count is floored at 1 and the single row has to squeeze; that
                // deliberate exception is covered by the viewport-too-short test instead.
                if ((screenHeight - breakpoint.chrome).value < breakpoint.rowMinHeight.value) {
                    return@forEach
                }

                val rows = breakpoint.rowsOnScreen(screenHeight)
                val rendered = breakpoint.renderedRowHeight(screenHeight, rows)

                assertTrue(
                    "${breakpoint.name} at $screenHeight: $rows rows of $rendered, " +
                        "under the ${breakpoint.rowMinHeight} minimum",
                    rendered.value >= breakpoint.rowMinHeight.value - 0.01f
                )
            }
        }
    }

    @Test
    fun `no page has room for another row`() {
        // The complement of the assertion above, and the actual #667 regression: leftover height
        // below a full page means the count was too low for the screen it rendered on.
        breakpoints.forEach { breakpoint ->
            (240..1600).forEach { height ->
                val screenHeight = height.dp
                val rows = breakpoint.rowsOnScreen(screenHeight)
                val gridHeight = screenHeight - breakpoint.chrome
                val oneMoreRow = breakpoint.rowMinHeight * (rows + 1) +
                    breakpoint.rowSpacing * rows

                assertTrue(
                    "${breakpoint.name} at $screenHeight: $rows rows leave room for a ${rows + 1}th",
                    oneMoreRow.value > gridHeight.value
                )
            }
        }
    }

    @Test
    fun `a viewport too short for one row still renders one`() {
        // Reachable in split-screen and on a foldable's cover display. One row that has to squeeze
        // beats an empty page with no way to pick a voice.
        breakpoints.forEach { breakpoint ->
            assertEquals(
                "${breakpoint.name} on a viewport shorter than its chrome",
                1,
                breakpoint.rowsOnScreen(0.dp)
            )
        }
    }
}

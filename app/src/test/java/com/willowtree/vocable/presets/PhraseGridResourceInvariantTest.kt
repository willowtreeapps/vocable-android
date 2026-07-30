package com.willowtree.vocable.presets

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The phrases grid renders a fixed [rows] x [columns] matrix and pages through items
 * [maxPhrases] at a time. If rows x columns != maxPhrases for any breakpoint, the grid
 * either shows a blank row/column (too many cells) or silently drops phrases that can
 * never be paged into view (too few cells) - see #611.
 */
class PhraseGridResourceInvariantTest {

    @Test
    fun `generic phrase grid dimensions equal max phrases for every breakpoint`() {
        ResourceXml.breakpointDirs.forEach { dir ->
            val integers = ResourceXml.integers(dir)
            val columns = integers.getValue("phrases_columns")
            val rows = integers.getValue("phrases_rows")
            val maxPhrases = integers.getValue("max_phrases")
            assertEquals(
                "$dir/integers.xml: phrases_columns ($columns) x phrases_rows ($rows) should equal max_phrases ($maxPhrases)",
                maxPhrases,
                columns * rows
            )
        }
    }

    @Test
    fun `keypad one-liner phrase grid dimensions equal max phrases for every breakpoint`() {
        ResourceXml.breakpointDirs.forEach { dir ->
            val integers = ResourceXml.integers(dir)
            val columns = integers.getValue("phrases_columns_one_liner_phrases")
            val rows = integers.getValue("phrases_rows_one_liner_phrases")
            val maxPhrases = integers.getValue("max_phrases_one_liner")
            assertEquals(
                "$dir/integers.xml: phrases_columns_one_liner_phrases ($columns) x phrases_rows_one_liner_phrases ($rows) should equal max_phrases_one_liner ($maxPhrases)",
                maxPhrases,
                columns * rows
            )
        }
    }
}

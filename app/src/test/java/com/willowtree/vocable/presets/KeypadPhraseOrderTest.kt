package com.willowtree.vocable.presets

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The "123" category is laid out row-major by PresetsScreen, so the declaration order of
 * R.array.category_123 is what produces the phone-keypad shape the designs call for:
 *
 *     3 columns          6 columns
 *     1 2 3              1 2 3 4 5 6
 *     4 5 6              7 8 9 0 No Yes
 *     7 8 9
 *     0 No Yes
 *
 * Order is also what seeds PresetPhrase.sort_order, so a reorder here changes what users see.
 */
class KeypadPhraseOrderTest {

    private val keypadOrder = listOf(
        "category_123_1",
        "category_123_2",
        "category_123_3",
        "category_123_4",
        "category_123_5",
        "category_123_6",
        "category_123_7",
        "category_123_8",
        "category_123_9",
        "category_123_0",
        "category_123_no",
        "category_123_yes"
    )

    @Test
    fun `keypad phrases are declared in phone-keypad order`() {
        assertEquals(
            keypadOrder,
            ResourceXml.stringArrayEntryNames("values", "category_123")
        )
    }

    /**
     * The layouts signed off in the designs, keyed by column count. A breakpoint that uses any
     * other column count has no approved keypad shape, so it has to be added here deliberately
     * rather than passing by accident - 4 columns, for instance, still fits all 12 keypad phrases
     * while rendering 1 2 3 4 / 5 6 7 8 / 9 0 No Yes.
     */
    private val approvedLayouts = mapOf(
        3 to listOf(
            listOf("category_123_1", "category_123_2", "category_123_3"),
            listOf("category_123_4", "category_123_5", "category_123_6"),
            listOf("category_123_7", "category_123_8", "category_123_9"),
            listOf("category_123_0", "category_123_no", "category_123_yes")
        ),
        6 to listOf(
            listOf(
                "category_123_1", "category_123_2", "category_123_3",
                "category_123_4", "category_123_5", "category_123_6"
            ),
            listOf(
                "category_123_7", "category_123_8", "category_123_9",
                "category_123_0", "category_123_no", "category_123_yes"
            )
        )
    )

    @Test
    fun `every breakpoint renders an approved keypad layout`() {
        ResourceXml.breakpointDirs.forEach { dir ->
            val integers = ResourceXml.integers(dir)
            val columns = integers.getValue("phrases_columns_one_liner_phrases")
            val rows = integers.getValue("phrases_rows_one_liner_phrases")

            val expected = approvedLayouts[columns]
                ?: throw AssertionError(
                    "$dir/integers.xml: $columns columns has no approved keypad layout - add one " +
                        "to approvedLayouts once the design is signed off"
                )
            assertEquals(
                "$dir/integers.xml: a $columns-column keypad needs ${expected.size} rows",
                expected.size,
                rows
            )
            assertEquals(
                "$dir/integers.xml: $columns columns x $rows rows does not render the keypad",
                expected,
                keypadOrder.chunked(columns)
            )
        }
    }
}

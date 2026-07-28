package com.willowtree.vocable.presets

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Reads res/ XML directly off disk so these stay plain JVM tests - no Robolectric or device
 * needed just to assert on values the resource compiler would otherwise hide behind R ids.
 */
internal object ResourceXml {

    /**
     * Every breakpoint that defines its own phrase grid dimensions, discovered from disk so a
     * newly added `values-*` dir is covered by the invariants without touching this file.
     */
    val breakpointDirs: List<String> by lazy {
        File("src/main/res")
            .listFiles { file: File -> file.isDirectory && file.name.startsWith("values") }
            .orEmpty()
            .filter { dir ->
                File(dir, "integers.xml").exists() &&
                    integers(dir.name).containsKey("phrases_columns")
            }
            .map { it.name }
            .sorted()
            .also { require(it.isNotEmpty()) { "No values*/integers.xml declared phrases_columns" } }
    }

    fun integers(dir: String): Map<String, Int> {
        val nodes = parse(dir, "integers.xml").getElementsByTagName("integer")
        return (0 until nodes.length).associate { i ->
            val element = nodes.item(i) as Element
            element.getAttribute("name") to element.textContent.trim().toInt()
        }
    }

    /**
     * Returns the resource entry names referenced by a string-array, in declaration order -
     * e.g. `@string/category_123_1` becomes `category_123_1`.
     */
    fun stringArrayEntryNames(dir: String, arrayName: String): List<String> {
        val arrays = parse(dir, "arrays.xml").getElementsByTagName("string-array")
        val array = (0 until arrays.length)
            .map { arrays.item(it) as Element }
            .single { it.getAttribute("name") == arrayName }
        val items = array.getElementsByTagName("item")
        return (0 until items.length).map { i ->
            items.item(i).textContent.trim().removePrefix("@string/")
        }
    }

    private fun parse(dir: String, fileName: String) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/$dir/$fileName"))
}

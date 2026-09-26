package dev.jotalac.feature.editor.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownChunkerTest {

    @Test
    fun chunkHeaderIntoBlocksTest() {
        // normal enter (return) press inserts two new lines
        val result = chunkMarkdownIntoBlocks("# h1\n\n# h2")

        assertEquals(2, result.size)
        assertEquals("# h1", result[0])
        assertEquals("# h2", result[1])
    }

    @Test
    fun chunkMarkdownWithNormalizedNewLinesTest() {
        val text = "# h1\r\n## h2\r### h3"

        val result = chunkMarkdownIntoBlocks(text)

        assertEquals(3, result.size)
        assertEquals("# h1", result[0])
        assertEquals("## h2", result[1])
        assertEquals("### h3", result[2])
    }

    @Test
    fun chunkMarkdownKeepsEmptyLinesTest() {
        val text = "# h1\n \n\n \n\n \n# h2"

        val result = chunkMarkdownIntoBlocks(text)

        assertEquals(4, result.size)
        assertEquals("# h1", result[0])
        assertEquals("", result[1])
        assertEquals("", result[2])
        assertEquals("# h2", result[3])
    }

    @Test
    fun chunkParagraphIntoBlocksTest() {
        val text = "First line\n  new line\n second\n third"

        val result = chunkMarkdownIntoBlocks(text)

        assertEquals(1, result.size)
    }

    @Test
    fun chunkListsIntoBlocksTest() {
        val text = "- item 1\n- item 2\n- item 3\n1. item\n2. item"

        val result = chunkMarkdownIntoBlocks(text)

        assertEquals(2, result.size)
    }

    @Test
    fun chunkCodeBlocksTest() {
        val text = """
           ```kotlin
           val test = "test"
           # h1 inside code block
           - list 
           - list
           ```
        """.trimIndent()

        val result = chunkMarkdownIntoBlocks(text)

        assertEquals(1, result.size)
    }

    @Test
    fun chunkEmptyText() {
        val result = chunkMarkdownIntoBlocks("")

        assertEquals(0, result.size)
    }
}

package dev.jotalac.feature.editor.ui

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EditorBlocksStateTest {

    private lateinit var blocksState: EditorBlocksState

    @BeforeTest
    fun setup() {
        blocksState = EditorBlocksState()
    }

    // blocks add test
    @Test
    fun addBlockAtLastIndexTest() {
        blocksState.addBlock(null)
        blocksState.addBlock(1)

        assertEquals(2, blocksState.blocks.size)
    }

    @Test
    fun addBlockAtUnexistingIndexTest() {
        blocksState.addBlock(1)
        blocksState.addBlock(50)
        blocksState.addBlock(10)

        assertEquals(0, blocksState.blocks.size)
    }


    @Test
    fun updateBlockTest() {
        blocksState.addBlock(null)
        blocksState.addBlock(null)

        blocksState.updateBlock(0, "test")
        blocksState.updateBlock(30, "test")

        assertEquals("test", blocksState.blocks[0])
        assertEquals("", blocksState.blocks[1])
    }

    @Test
    fun removeBlockTest() {
        blocksState.addBlock(null)
        blocksState.addBlock(null)
        blocksState.addBlock(null)

        blocksState.updateBlock(2, "test")

        blocksState.removeBlock(1)
        blocksState.removeBlock(10)

        assertEquals(2, blocksState.blocks.size)
        assertEquals("test", blocksState.blocks[1])
    }

    @Test
    fun addBlocksTest() {
        blocksState.addBlocks(0, listOf("one", "two", "three"))
        blocksState.addBlocks(0, listOf("zero"))

        assertEquals(4, blocksState.blocks.size)
        assertEquals("zero", blocksState.blocks[0])
        assertEquals("one", blocksState.blocks[1])
        assertEquals("two", blocksState.blocks[2])
        assertEquals("three", blocksState.blocks[3])
    }

    @Test
    fun replaceBlockWithBlocksTest() {
        blocksState.addBlock(null)

        // unexisting index doesnt replace anything
        blocksState.replaceBlockWithBlocks(10, listOf())
        assertEquals(1, blocksState.blocks.size)

        // existing index replaces block
        blocksState.replaceBlockWithBlocks(0, listOf("one", "two"))
        assertEquals(2, blocksState.blocks.size)
        assertEquals("one", blocksState.blocks[0])
        assertEquals("two", blocksState.blocks[1])
    }

    @Test
    fun splitBlockTest() {
        blocksState.addBlock(null)
        blocksState.updateBlock(0, "# first\n# second\n# third")

        blocksState.splitBlock(0, 7)

        assertEquals(3, blocksState.blocks.size)
        assertEquals("# first", blocksState.blocks[0])
        assertEquals("# second", blocksState.blocks[1])
        assertEquals("# third", blocksState.blocks[2])
    }

    @Test
    fun splitBlockTest2() {
        blocksState.addBlock(null)
        blocksState.updateBlock(0, "random_text")

        blocksState.splitBlock(0, 0)

        assertEquals(2, blocksState.blocks.size)
        assertEquals("", blocksState.blocks[0])
        assertEquals("random_text", blocksState.blocks[1])
    }

    @Test
    fun splitBlockTest3() {
        blocksState.addBlock(null)
        blocksState.updateBlock(0, "- test\n- test\n- test")

        blocksState.splitBlock(0, 7)

        assertEquals(2, blocksState.blocks.size)
        assertEquals("- test", blocksState.blocks[0])
        assertEquals("- test\n- test", blocksState.blocks[1])
    }

    @Test
    fun mergeWithPreviousTest() {
        blocksState.addBlock(null)
        blocksState.addBlock(null)
        blocksState.updateBlock(0, "te")
        blocksState.updateBlock(1, "st")

        blocksState.mergeWithPrevious(1)
        blocksState.mergeWithPrevious(0) // doesnt do anything

        assertEquals(1, blocksState.blocks.size)
        assertEquals("test", blocksState.blocks[0])
    }

    @Test
    fun evaluateBlockOnFocusLostTest() {
        blocksState.addBlock(null)
        blocksState.addBlock(null)
        blocksState.updateBlock(1, "# header\n# header two")

        // this deletes the empty block
        blocksState.evaluateBlockOnFocusLost(0, 0)

        assertEquals(2, blocksState.blocks.size)

        assertNull(blocksState.evaluateBlockOnFocusLost(0, 0))
    }

    @Test
    fun swapBlocksTest() {
        blocksState.setBlocks(listOf("zero", "one"))

        blocksState.swapBlocks(0, 1)
        blocksState.swapBlocks(10, 80)
        blocksState.swapBlocks(1, 1)

        assertEquals(2, blocksState.blocks.size)
        assertEquals("one", blocksState.blocks[0])
        assertEquals("zero", blocksState.blocks[1])
    }

    @Test
    fun setBlocksTest() {
        blocksState.addBlock(null)
        blocksState.addBlock(null)

        blocksState.setBlocks(listOf())
        assertEquals(0, blocksState.blocks.size)

        blocksState.setBlocks(listOf("one", "two"))
        assertEquals(2, blocksState.blocks.size)
        assertEquals("one", blocksState.blocks[0])
        assertEquals("two", blocksState.blocks[1])
    }

    @Test
    fun insertImageBlocksTest() {
        blocksState
    }
}
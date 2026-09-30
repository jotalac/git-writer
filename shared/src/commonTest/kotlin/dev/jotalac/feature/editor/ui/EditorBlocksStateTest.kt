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
    fun addNegativeIndexBlock() {
        blocksState.addBlock(-67)

        assertEquals(0, blocksState.blocks.size)
    }




    @Test
    fun updateBlockTest() {
        blocksState.addBlock(null)
        blocksState.addBlock(null)

        blocksState.updateBlock(0, "test")
        blocksState.updateBlock(1, "test2")
        blocksState.updateBlock(30, "test")
        blocksState.updateBlock(-20 , "test")

        assertEquals("test", blocksState.blocks[0])
        assertEquals("test2", blocksState.blocks[1])
    }

    @Test
    fun removeBlockTest() {
        blocksState.addBlock(null)
        blocksState.addBlock(null)
        blocksState.addBlock(null)

        blocksState.updateBlock(2, "test")

        blocksState.removeBlock(1)
        blocksState.removeBlock(10)
        blocksState.removeBlock(10)

        assertEquals(2, blocksState.blocks.size)
        assertEquals("test", blocksState.blocks[1])
    }

    @Test
    fun removeBlockNegativeIndexTest() {
        blocksState.addBlock(null)

        blocksState.removeBlock(-1)

        assertEquals(1, blocksState.blocks.size)
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
    fun addBlocksAtSizeTest() {
        blocksState.addBlock(null)
        blocksState.addBlocks(1, listOf("test"))

        assertEquals(2, blocksState.blocks.size)
    }

    @Test
    fun replaceBlockWithBlocksTest() {
        blocksState.addBlock(null)
        blocksState.addBlock(null)

        // unexisting index doesnt replace anything
        blocksState.replaceBlockWithBlocks(10, listOf())
        assertEquals(2, blocksState.blocks.size)

        // existing index replaces block
        blocksState.replaceBlockWithBlocks(1, listOf("one", "two"))
        assertEquals(3, blocksState.blocks.size)
        assertEquals("one", blocksState.blocks[1])
        assertEquals("two", blocksState.blocks[2])
    }

    @Test
    fun splitBlockTest() {
        blocksState.addBlock(null)
        blocksState.updateBlock(0, "# first\n# second\n# third")

        val resultIndex = blocksState.splitBlock(0, 7)

        assertEquals(1, resultIndex)

        assertEquals(3, blocksState.blocks.size)
        assertEquals("# first", blocksState.blocks[0])
        assertEquals("# second", blocksState.blocks[1])
        assertEquals("# third", blocksState.blocks[2])
    }

    @Test
    fun splitBlockTest2() {
        blocksState.addBlock(null)
        blocksState.updateBlock(0, "random_text")

        val resultIndex = blocksState.splitBlock(0, 0)

        assertEquals(1, resultIndex)

        assertEquals(2, blocksState.blocks.size)
        assertEquals("", blocksState.blocks[0])
        assertEquals("random_text", blocksState.blocks[1])
    }

    @Test
    fun splitBlockTest3() {
        blocksState.addBlock(null)
        blocksState.updateBlock(0, "- test\n- test\n- test")

        val resultIndex = blocksState.splitBlock(0, 7)

        assertEquals(1, resultIndex)

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
        blocksState.mergeWithPrevious(10) // unexisting index
        blocksState.mergeWithPrevious(-50) // negative index

        assertEquals(1, blocksState.blocks.size)
        assertEquals("test", blocksState.blocks[0])
    }

    @Test
    fun evaluateBlockOnFocusLostTest() {
        blocksState.addBlock(null)
        blocksState.addBlock(null)
        blocksState.updateBlock(0, "# a\n# b")

        // block 0 becomes two blocks, so the block that keeps the focus (index 1) moves to index 2
        val resultIndex = blocksState.evaluateBlockOnFocusLost(0, 1)

        assertEquals(2, resultIndex)
        assertEquals(3, blocksState.blocks.size)
        assertEquals("# a", blocksState.blocks[0])
        assertEquals("# b", blocksState.blocks[1])
        assertEquals("", blocksState.blocks[2])
    }

    @Test
    fun evaluateBlockOnNegativeIndexLostTest() {
        blocksState.addBlock(null)
        val resultIndex = blocksState.evaluateBlockOnFocusLost(-1, 1)


        assertNull(resultIndex)
        assertEquals(1, blocksState.blocks.size)
    }

    @Test
    fun evaluateBlockOnFocusLostSingleChunkReturnsNullTest() {
        blocksState.setBlocks(listOf("one", "two"))

        // "one" stays a single block, so nothing needs re-indexing
        val resultIndex = blocksState.evaluateBlockOnFocusLost(0, 1)

        assertNull(resultIndex)
        assertEquals(2, blocksState.blocks.size)
        assertEquals("one", blocksState.blocks[0])
        assertEquals("two", blocksState.blocks[1])
    }

    @Test
    fun evaluateBlockOnFocusLostSplitsWhenFocusedBlockIsNotAfterItTest() {
        blocksState.addBlock(null)
        blocksState.updateBlock(0, "# a\n# b")

        // the block is still split, but the focused block sits before it, so its index does not shift
        val resultIndex = blocksState.evaluateBlockOnFocusLost(0, 0)

        assertNull(resultIndex)
        assertEquals(2, blocksState.blocks.size)
        assertEquals("# a", blocksState.blocks[0])
        assertEquals("# b", blocksState.blocks[1])
    }

    @Test
    fun evaluateBlockOnFocusLostWithoutFocusedBlockReturnsNullTest() {
        blocksState.addBlock(null)
        blocksState.updateBlock(0, "# a\n# b")

        // nothing else holds the focus, so there is no index to repair
        val resultIndex = blocksState.evaluateBlockOnFocusLost(0, null)

        assertNull(resultIndex)
        assertEquals(2, blocksState.blocks.size)
    }

    @Test
    fun evaluateBlockOnFocusLostOutOfRangeIndexReturnsNullTest() {
        blocksState.setBlocks(listOf("one"))

        val resultIndex = blocksState.evaluateBlockOnFocusLost(5, null)

        assertNull(resultIndex)
        assertEquals(1, blocksState.blocks.size)
    }

    @Test
    fun swapBlocksTest() {
        blocksState.setBlocks(listOf("zero", "one"))

        blocksState.swapBlocks(0, 1)
        blocksState.swapBlocks(10, 80)
        blocksState.swapBlocks(1, 1)
        blocksState.swapBlocks(0, 10)

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
    fun insertImagesOnEmptyTest() {
        blocksState.addBlock(null)
        val imageMarkdown = listOf("image_one", "image_two")
        val focusedIndex = 0

        val returnIndex = blocksState.insertImageBlocks(imageMarkdown, focusedIndex)

        assertEquals(2, returnIndex)

        // image, image, our active empty block
        assertEquals(3, blocksState.blocks.size)
        assertEquals("image_one", blocksState.blocks[0])
        assertEquals("image_two", blocksState.blocks[1])
    }

    @Test
    fun insertImagesInMiddleTest() {
        blocksState.addBlock(null)
        blocksState.addBlock(null)
        blocksState.updateBlock(0,"text1")
        blocksState.updateBlock(1,"text2")

        val imageMarkdown = listOf("image_one", "image_two")

        val returnIndex = blocksState.insertImageBlocks(imageMarkdown, 0)

        assertEquals(3, returnIndex)

        assertEquals(5, blocksState.blocks.size)
        assertEquals("text1", blocksState.blocks[0])
        assertEquals("image_one", blocksState.blocks[1])
        assertEquals("image_two", blocksState.blocks[2])
        assertEquals("", blocksState.blocks[3])
        assertEquals("text2", blocksState.blocks[4])
    }

    @Test
    fun insertImagesEmptyListTest() {
        val returnIndex = blocksState.insertImageBlocks(emptyList(), 0)

        assertEquals(0, returnIndex)
    }
}
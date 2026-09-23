package dev.jotalac.feature.editor.ui

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorBlocksStateTest {

    private lateinit var blocksState: EditorBlocksState

    @BeforeTest
    fun setup() {
        blocksState = EditorBlocksState()
    }

    @Test
    fun addBlockIndexNullTest() {
        blocksState.addBlock(null)

        assertEquals(1, blocksState.blocks.size)
    }

    @Test
    fun addMultipleBlocksTest() {
        blocksState.addBlock(null)
        blocksState.addBlock(null)
        blocksState.addBlock(null)

        assertEquals(3, blocksState.blocks.size)
    }

    @Test
    fun updateBlockTest {

    }
}
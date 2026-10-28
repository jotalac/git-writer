package dev.jotalac.feature.git_sync.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GitBundleFormatTest {

    private val head = "fd707dfee68952dfd51b590cccef17530c65ed1a"
    private val earlier = "33c932f7da488d105b3cecdb155d6058a26e1198"

    @Test
    fun `a written header reads back the same`() {
        val bundle = bundleHeader(head, prerequisite = earlier) + "PACK".encodeToByteArray()

        val header = parseBundleHeader(bundle)

        assertEquals(mapOf(BUNDLE_MAIN_REF to head), header.refs)
        assertEquals(listOf(earlier), header.prerequisites)
        assertEquals("PACK", bundle.copyOfRange(header.packOffset, bundle.size).decodeToString())
    }

    @Test
    fun `the header git writes is read - also its comments on the commits a bundle builds on`() {
        // "-<commit> first" - git adds the commit's subject
        val header = parseBundleHeader(GitBundleFixtures.NEXT)

        assertEquals(listOf(GitBundleFixtures.FIRST_COMMIT), header.prerequisites)
        assertEquals(mapOf(BUNDLE_MAIN_REF to GitBundleFixtures.SECOND_COMMIT), header.refs)
        assertEquals(
            "PACK",
            GitBundleFixtures.NEXT.copyOfRange(header.packOffset, header.packOffset + 4).decodeToString(),
        )
    }

    @Test
    fun `a bundle without anything to build on has no prerequisites`() {
        val header = parseBundleHeader(GitBundleFixtures.FULL)

        assertEquals(emptyList(), header.prerequisites)
        assertEquals(mapOf(BUNDLE_MAIN_REF to GitBundleFixtures.FIRST_COMMIT), header.refs)
    }

    @Test
    fun `anything that is not a bundle is refused`() {
        assertFailsWith<IllegalArgumentException> { parseBundleHeader("# Diary\n\ntext".encodeToByteArray()) }
        // a header that never ends - cut off in the middle
        assertFailsWith<IllegalArgumentException> { parseBundleHeader("# v2 git bundle\n$head ref".encodeToByteArray()) }
    }
}

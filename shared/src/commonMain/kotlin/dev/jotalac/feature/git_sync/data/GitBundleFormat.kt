package dev.jotalac.feature.git_sync.data

/*
* Git's bundle file (v2), as `git bundle create` writes it: a text header, then a packfile.
*
*   # v2 git bundle
*   -<commit> [comment]      a commit the bundle builds on - the repository it goes into must have it
*   <commit> <ref>           what a ref points at after the bundle is imported
*   (empty line)
*   PACK...                  the objects
*
* JGit reads and writes it itself, libgit2 only has the packfile part - this is the rest.
* */

const val BUNDLE_MAIN_REF = "refs/heads/main"
private const val SIGNATURE = "# v2 git bundle\n"

/** A bundle's header: the refs it sets, the commits it builds on, and where its packfile starts. */
data class BundleHeader(val refs: Map<String, String>, val prerequisites: List<String>, val packOffset: Int)

/** The header of a bundle that points `main` at [head] and builds on [prerequisite], if any. */
fun bundleHeader(head: String, prerequisite: String?): ByteArray = buildString {
    append(SIGNATURE)
    prerequisite?.let { append("-$it\n") }
    append("$head $BUNDLE_MAIN_REF\n\n")
}.encodeToByteArray()

/** Reads the header of [bundle]. Throws [IllegalArgumentException] for anything that is not a v2 bundle. */
fun parseBundleHeader(bundle: ByteArray): BundleHeader {
    val signature = SIGNATURE.encodeToByteArray()
    require(bundle.size >= signature.size && bundle.copyOfRange(0, signature.size).contentEquals(signature)) {
        "not a git bundle (v2)"
    }

    val refs = mutableMapOf<String, String>()
    val prerequisites = mutableListOf<String>()
    var lineStart = signature.size
    while (true) {
        val lineEnd = (lineStart until bundle.size).firstOrNull { bundle[it] == '\n'.code.toByte() }
            ?: throw IllegalArgumentException("the bundle header does not end")
        val line = bundle.copyOfRange(lineStart, lineEnd).decodeToString()
        lineStart = lineEnd + 1
        when {
            line.isEmpty() -> return BundleHeader(refs, prerequisites, packOffset = lineStart)
            // "-<commit> <comment>" - the comment is optional
            line.startsWith("-") -> prerequisites += line.drop(1).substringBefore(" ")
            else -> refs[line.substringAfter(" ")] = line.substringBefore(" ")
        }
    }
}

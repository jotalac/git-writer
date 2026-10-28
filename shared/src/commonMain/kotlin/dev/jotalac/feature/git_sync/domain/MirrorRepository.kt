package dev.jotalac.feature.git_sync.domain

/**
 * The mirror of an encrypted notebook: a bare repository on the device holding, decrypted, everything that was
 * published - the notebook's git remote. Commits go in and out of it as bundles, git's file format for what a push
 * sends. When a notebook becomes encrypted, its own repository is bundled too.
 */
interface MirrorRepository {

    /** A new, empty mirror at [path]. */
    suspend fun create(path: String): Result<Unit>

    /** The commit `main` of the repository at [path] points at, null while it has none. */
    suspend fun head(path: String): Result<String?>

    /** The commits on `main` of the repository at [path] that [since] (a commit, null for all) does not have, as a bundle. */
    suspend fun createBundle(path: String, since: String?): Result<ByteArray>

    /**
     * Adds the commits of [bundle] to the repository at [path] - which has to have those the bundle builds on - and
     * points `main` at the bundle's head. Returns that head.
     */
    suspend fun importBundle(path: String, bundle: ByteArray): Result<String>

    /** Renames the current branch of the repository at [path] to `main` - bundles carry `main`. */
    suspend fun useMainBranch(path: String): Result<Unit>
}

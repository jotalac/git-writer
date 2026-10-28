@file:OptIn(ExperimentalForeignApi::class)

package dev.jotalac.feature.git_sync.data.libgit2

import cnames.structs.git_commit
import cnames.structs.git_index
import cnames.structs.git_reference
import cnames.structs.git_repository
import git2.GIT_ENOTFOUND
import git2.GIT_REPOSITORY_INIT_BARE
import git2.GIT_REPOSITORY_INIT_MKPATH
import git2.GIT_REPOSITORY_INIT_OPTIONS_VERSION
import git2.git_commit_free
import git2.git_commit_lookup
import git2.git_config_free
import git2.git_config_set_string
import git2.git_index_free
import git2.git_oid
import git2.git_reference_free
import git2.git_reference_lookup
import git2.git_reference_name_to_id
import git2.git_reference_symbolic_target
import git2.git_repository_config
import git2.git_repository_free
import git2.git_repository_index
import git2.git_repository_init_ext
import git2.git_repository_init_options
import git2.git_repository_init_options_init
import git2.git_repository_open
import git2.git_repository_state
import git2.git_repository_state_t
import git2.git_repository_workdir
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

/**
 * An open libgit2 repository - opened for one operation and closed after it, so use it with `use`.
 *
 * What can be done with it is next to it, by topic: GitCommits, GitMerge, GitConflicts, GitRemotes and
 * GitNetwork. They build on the lookups here.
 */
internal class GitRepository internal constructor(val handle: CPointer<git_repository>) : AutoCloseable {

    override fun close() = git_repository_free(handle)

    /** The working tree, ending with a slash. */
    val workdir: String
        get() = git_repository_workdir(handle)?.toKString() ?: throw GitException("the repository has no working tree")

    /** A merge that ran into conflicts, and is neither completed nor aborted yet. */
    val isMerging: Boolean
        get() = git_repository_state(handle) == git_repository_state_t.GIT_REPOSITORY_STATE_MERGE.value.toInt()

    /** The branch HEAD points at ("main"), also when it has no commit yet. Null for a detached HEAD. */
    val currentBranch: String?
        get() = memScoped {
            // read from HEAD itself - resolving it fails while the branch has no commit
            val head = create(::git_reference_free) { git_reference_lookup(it, handle, "HEAD") }
            git_reference_symbolic_target(head)?.toKString()?.removePrefix("refs/heads/")
        }

    fun hasReference(name: String): Boolean = memScoped { findReference(name) != null }

    fun setConfig(key: String, value: String): Unit = memScoped {
        val config = create(::git_config_free) { git_repository_config(it, handle) }
        checkGit(git_config_set_string(config, key, value))
    }

    // --- lookups, freed when the memory scope ends

    fun MemScope.index(): CPointer<git_index> = create(::git_index_free) { git_repository_index(it, handle) }

    /** The reference [name] ("refs/heads/main"), null when it does not exist. */
    fun MemScope.findReference(name: String): CPointer<git_reference>? =
        lookup(::git_reference_free) { git_reference_lookup(it, handle, name) }

    /** The commit the reference [name] ("HEAD") points at, null when there is none (yet). */
    fun MemScope.findCommit(name: String): CPointer<git_commit>? {
        val id = alloc<git_oid>()
        val result = git_reference_name_to_id(id.ptr, handle, name)
        if (result == GIT_ENOTFOUND) return null
        checkGit(result)
        return create(::git_commit_free) { git_commit_lookup(it, handle, id.ptr) }
    }

    companion object {

        fun open(path: String): GitRepository = openWith { git_repository_open(it, path) }

        fun init(path: String, initialBranch: String, bare: Boolean = false): GitRepository = memScoped {
            val options = alloc<git_repository_init_options>()
            checkGit(git_repository_init_options_init(options.ptr, GIT_REPOSITORY_INIT_OPTIONS_VERSION.convert()))
            options.flags = if (bare) GIT_REPOSITORY_INIT_MKPATH or GIT_REPOSITORY_INIT_BARE else GIT_REPOSITORY_INIT_MKPATH
            options.initial_head = initialBranch.cstr.ptr

            openWith { git_repository_init_ext(it, path, options.ptr) }
        }

        /** Wraps the repository a libgit2 call opens or creates through its out-parameter. */
        internal inline fun openWith(call: (CPointer<CPointerVar<git_repository>>) -> Int): GitRepository = memScoped {
            val out = alloc<CPointerVar<git_repository>>()
            checkGit(call(out.ptr))
            GitRepository(out.value ?: throw GitException("libgit2 opened no repository"))
        }
    }
}

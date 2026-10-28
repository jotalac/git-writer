@file:OptIn(ExperimentalForeignApi::class)

package dev.jotalac.feature.git_sync.data.libgit2

import dev.jotalac.feature.git_sync.data.BUNDLE_MAIN_REF
import dev.jotalac.feature.git_sync.data.bundleHeader
import dev.jotalac.feature.git_sync.data.parseBundleHeader
import git2.git_branch_move
import git2.git_buf
import git2.git_buf_dispose
import git2.git_commit_free
import git2.git_commit_lookup
import git2.git_indexer_append
import git2.git_indexer_commit
import git2.git_indexer_free
import git2.git_indexer_new
import git2.git_indexer_progress
import git2.git_odb_free
import git2.git_odb_refresh
import git2.git_oid
import git2.git_oid_fromstr
import git2.git_oid_tostr_s
import git2.git_packbuilder_free
import git2.git_packbuilder_insert_walk
import git2.git_packbuilder_new
import git2.git_packbuilder_write_buf
import git2.git_reference_create
import git2.git_reference_free
import git2.git_reference_name_to_id
import git2.git_repository_odb
import git2.git_repository_path
import git2.git_repository_set_head
import git2.git_revwalk_free
import git2.git_revwalk_hide
import git2.git_revwalk_new
import git2.git_revwalk_push_ref
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned

/*
* Git bundles - what a push sends, as a file: libgit2 builds and indexes the packfile, GitBundleFormat the header.
* */

/** Renames the current branch to [name] - also one without commits yet, which only HEAD names. */
internal fun GitRepository.renameCurrentBranch(name: String): Unit = memScoped {
    val current = currentBranch ?: throw GitException("HEAD is detached, there is no branch to rename")
    if (current == name) return
    val branch = findReference("refs/heads/$current")
    if (branch == null) {
        checkGit(git_repository_set_head(handle, "refs/heads/$name"))
    } else {
        // also moves HEAD and the branch's config (its upstream) along
        create(::git_reference_free) { git_branch_move(it, branch, name, 0) }
    }
}

/** The commit `main` points at, null while it has none. */
internal fun GitRepository.mainHead(): String? = memScoped {
    val id = alloc<git_oid>()
    val result = git_reference_name_to_id(id.ptr, handle, BUNDLE_MAIN_REF)
    if (result == git2.GIT_ENOTFOUND) return null
    checkGit(result)
    git_oid_tostr_s(id.ptr)?.toKString()
}

/** The commits on `main` that [since] (a commit, null for all) does not have, as a bundle. */
internal fun GitRepository.createBundle(since: String?): ByteArray = memScoped {
    val head = mainHead() ?: throw GitException("main has no commits to bundle")

    val walk = create(::git_revwalk_free) { git_revwalk_new(it, handle) }
    checkGit(git_revwalk_push_ref(walk, BUNDLE_MAIN_REF))
    if (since != null) checkGit(git_revwalk_hide(walk, oid(since)))

    val packBuilder = create(::git_packbuilder_free) { git_packbuilder_new(it, handle) }
    checkGit(git_packbuilder_insert_walk(packBuilder, walk))
    val pack = alloc<git_buf>().apply { ptr = null; reserved = 0u; size = 0u }
    // the struct's address - `pack.ptr` is git_buf's own field, the data
    val packAddress = interpretCPointer<git_buf>(pack.rawPtr)
    checkGit(git_packbuilder_write_buf(packAddress, packBuilder))
    try {
        bundleHeader(head, since) + (pack.ptr?.readBytes(pack.size.toInt()) ?: ByteArray(0))
    } finally {
        git_buf_dispose(packAddress)
    }
}

/**
 * Adds the commits of [bundle] to the repository - it has to have those the bundle builds on - and points `main` at
 * the bundle's head. Returns that head.
 */
internal fun GitRepository.importBundle(bundle: ByteArray): String = memScoped {
    val header = parseBundleHeader(bundle)
    val head = header.refs[BUNDLE_MAIN_REF] ?: throw GitException("the bundle does not carry $BUNDLE_MAIN_REF")

    // like git: the commits the bundle builds on have to be here - its commits only point at them, so the pack
    // would import fine and leave main on a history with a hole in it
    header.prerequisites.forEach { commit ->
        lookup(::git_commit_free) { git_commit_lookup(it, handle, oid(commit)) }
            ?: throw GitException("the repository does not have the commit $commit this bundle builds on")
    }

    // the packfile goes where git keeps packs - the repository's objects can complete a thin one
    val odb = create(::git_odb_free) { git_repository_odb(it, handle) }
    val packDirectory = git_repository_path(handle)!!.toKString() + "objects/pack"
    val indexer = create(::git_indexer_free) { git_indexer_new(it, packDirectory, 0u, odb, null) }
    val progress = alloc<git_indexer_progress>()
    bundle.usePinned { pinned ->
        val packSize = bundle.size - header.packOffset
        checkGit(git_indexer_append(indexer, pinned.addressOf(header.packOffset), packSize.convert(), progress.ptr))
    }
    checkGit(git_indexer_commit(indexer, progress.ptr))
    checkGit(git_odb_refresh(odb))

    // the head has to be there now - a bundle with commits missing before it would leave main broken
    val headId = oid(head)
    create(::git_commit_free) { git_commit_lookup(it, handle, headId) }
    create(::git_reference_free) { git_reference_create(it, handle, BUNDLE_MAIN_REF, headId, 1, "import bundle") }
    head
}

private fun MemScope.oid(commit: String) = alloc<git_oid>().also { checkGit(git_oid_fromstr(it.ptr, commit)) }.ptr

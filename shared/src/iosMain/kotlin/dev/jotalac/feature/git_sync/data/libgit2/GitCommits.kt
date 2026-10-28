@file:OptIn(ExperimentalForeignApi::class)

package dev.jotalac.feature.git_sync.data.libgit2

import git2.GIT_INDEX_ADD_DEFAULT
import git2.GIT_RESET_HARD
import git2.GIT_STATUS_OPTIONS_VERSION
import git2.GIT_STATUS_OPT_INCLUDE_UNTRACKED
import git2.GIT_STATUS_OPT_RECURSE_UNTRACKED_DIRS
import git2.GIT_STATUS_SHOW_INDEX_AND_WORKDIR
import git2.git_commit_create
import git2.git_index_add_all
import git2.git_index_update_all
import git2.git_index_write
import git2.git_index_write_tree
import git2.git_object_free
import git2.git_oid
import git2.git_repository_state_cleanup
import git2.git_reset
import git2.git_revparse_single
import git2.git_signature_default
import git2.git_signature_free
import git2.git_signature_now
import git2.git_status_list_entrycount
import git2.git_status_list_free
import git2.git_status_list_new
import git2.git_status_options
import git2.git_status_options_init
import git2.git_tree_free
import git2.git_tree_lookup
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr

/** Whether the working tree has anything to commit - new, edited or deleted files. */
internal fun GitRepository.hasChanges(): Boolean = memScoped {
    val options = alloc<git_status_options>()
    checkGit(git_status_options_init(options.ptr, GIT_STATUS_OPTIONS_VERSION.convert()))
    options.show = GIT_STATUS_SHOW_INDEX_AND_WORKDIR
    options.flags = GIT_STATUS_OPT_INCLUDE_UNTRACKED or GIT_STATUS_OPT_RECURSE_UNTRACKED_DIRS

    val status = create(::git_status_list_free) { git_status_list_new(it, handle, options.ptr) }
    git_status_list_entrycount(status) > 0u
}

/** Commits everything that changed in the working tree (`git add -A` and `git commit`). False when nothing did. */
internal fun GitRepository.commitAll(message: String): Boolean {
    if (!hasChanges()) return false

    memScoped {
        val index = index()
        // new and edited files, then the deleted ones
        checkGit(git_index_add_all(index, null, GIT_INDEX_ADD_DEFAULT, null, null))
        checkGit(git_index_update_all(index, null, null, null))
        checkGit(git_index_write(index))
    }
    commit(message)
    return true
}

/**
 * Commits what is staged on top of HEAD. During a merge the merged commit becomes the second parent, which
 * completes the merge.
 */
internal fun GitRepository.commit(message: String): Unit = memScoped {
    val treeId = alloc<git_oid>()
    checkGit(git_index_write_tree(treeId.ptr, index()))
    val tree = create(::git_tree_free) { git_tree_lookup(it, handle, treeId.ptr) }

    val merging = isMerging
    // none before the first commit
    val parents = listOfNotNull(findCommit("HEAD"), if (merging) findCommit("MERGE_HEAD") else null)

    // the user.name and user.email of the git config - the app sets none, so mostly the fallback
    val signature = lookup(::git_signature_free) { git_signature_default(it, handle) }
        ?: create(::git_signature_free) { git_signature_now(it, "Git Writer", "gitwriter@localhost") }

    val commitId = alloc<git_oid>()
    checkGit(
        git_commit_create(
            commitId.ptr, handle, "HEAD", signature, signature, null, message, tree,
            parents.size.convert(), allocArrayOf(parents),
        )
    )

    // the merge is done - forget its state (MERGE_HEAD, MERGE_MSG)
    if (merging) checkGit(git_repository_state_cleanup(handle))
}

/**
 * Throws away the changes in the working tree and the index, also a merge in progress, and points the branch at
 * [target] - HEAD keeps it where it is (`git reset --hard <target>`).
 */
internal fun GitRepository.resetHard(target: String = "HEAD"): Unit = memScoped {
    val commit = create(::git_object_free) { git_revparse_single(it, handle, target) }
    checkGit(git_reset(handle, commit, GIT_RESET_HARD, null))
}

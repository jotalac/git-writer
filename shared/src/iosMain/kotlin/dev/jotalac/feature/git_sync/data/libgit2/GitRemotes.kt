@file:OptIn(ExperimentalForeignApi::class)

package dev.jotalac.feature.git_sync.data.libgit2

import git2.git_graph_ahead_behind
import git2.git_reference_target
import git2.git_remote_create
import git2.git_remote_delete
import git2.git_remote_free
import git2.git_remote_list
import git2.git_remote_set_url
import git2.git_reference_list
import git2.git_strarray
import git2.git_strarray_dispose
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.posix.size_tVar

/*
* The remotes a repository knows - talking to them is in GitNetwork.
* */

internal fun GitRepository.remoteNames(): List<String> = memScoped {
    val names = alloc<git_strarray>()
    checkGit(git_remote_list(names.ptr, handle))
    try {
        List(names.count.toInt()) { names.strings?.get(it)?.toKString() }.filterNotNull()
    } finally {
        git_strarray_dispose(names.ptr)
    }
}

/** Whether a fetch from [remote] brought in any branch - each one has a remote-tracking ref, whatever its name. */
internal fun GitRepository.hasRemoteBranches(remote: String): Boolean = memScoped {
    val names = alloc<git_strarray>()
    checkGit(git_reference_list(names.ptr, handle))
    try {
        val prefix = "refs/remotes/$remote/"
        List(names.count.toInt()) { names.strings?.get(it)?.toKString() }.any { it?.startsWith(prefix) == true }
    } finally {
        git_strarray_dispose(names.ptr)
    }
}

internal fun GitRepository.addRemote(name: String, url: String): Unit = memScoped {
    create(::git_remote_free) { git_remote_create(it, handle, name, url) }
}

internal fun GitRepository.setRemoteUrl(name: String, url: String) {
    checkGit(git_remote_set_url(handle, name, url))
}

/** Removes the remote, with its remote-tracking branches and the upstream config of the branches using it. */
internal fun GitRepository.deleteRemote(name: String) {
    checkGit(git_remote_delete(handle, name))
}

/** Makes [remote] the upstream of the current branch (`git push -u`), as a clone sets it up. */
internal fun GitRepository.setUpstream(remote: String) {
    val branch = currentBranch ?: return
    setConfig("branch.$branch.remote", remote)
    setConfig("branch.$branch.merge", "refs/heads/$branch")
}

/** Whether the current branch has commits that [remote] did not have at the last fetch. */
internal fun GitRepository.hasUnpushedCommits(remote: String): Boolean = memScoped {
    val branch = currentBranch ?: return false
    // no commit yet
    val local = findReference("refs/heads/$branch") ?: return false
    // the remote does not have the branch at all
    val upstream = findReference("refs/remotes/$remote/$branch") ?: return true

    val ahead = alloc<size_tVar>()
    val behind = alloc<size_tVar>()
    checkGit(git_graph_ahead_behind(ahead.ptr, behind.ptr, handle, git_reference_target(local), git_reference_target(upstream)))
    ahead.value > 0u
}

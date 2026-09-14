@file:OptIn(ExperimentalForeignApi::class)

package dev.jotalac.feature.git_sync.data

import cnames.structs.git_index_conflict_iterator
import cnames.structs.git_repository
import dev.jotalac.core.utils.suspendRunCatching
import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import git2.GIT_REPOSITORY_INIT_OPTIONS_VERSION
import git2.git_repository_free
import git2.git_repository_init_ext
import git2.git_repository_init_options
import git2.git_repository_init_options_init
import git2.git_repository_state_cleanup
import git2.gitw_branch_set_upstream
import git2.gitw_branch_unset_upstream
import git2.gitw_clone
import git2.gitw_commit_if_dirty
import git2.gitw_commit_merge
import git2.gitw_conflict_iterator_new
import git2.gitw_conflict_next_path
import git2.gitw_fetch
import git2.gitw_merge_in_progress
import git2.gitw_index_conflict_count
import git2.gitw_ls_remote
import git2.gitw_merge_upstream
import git2.gitw_push
import git2.gitw_remote_create
import git2.gitw_remote_delete
import git2.gitw_remote_list_first
import git2.gitw_remote_set_url
import git2.gitw_reset_hard
import git2.gitw_resolve_conflicts_ours
import git2.gitw_resolve_conflicts_theirs
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

private const val DEFAULT_REMOTE = "origin"

/** Longest branch/remote name libgit2 will produce, plus room for a NUL. */
private const val NAME_BUFFER = 256

/** Longest worktree path we will build (matches GITW_PATH_MAX in git2.def). */
private const val PATH_BUFFER = 1024

/** iOS [GitSyncRepository] on libgit2; mirrors the JGit implementation. */
class LibGit2SyncRepositoryImpl : GitSyncRepository {

    init {
        LibGit2.ensureInitialised()
    }

    override suspend fun validateCredentials(
        repoUrl: String,
        tokenOrPassword: String,
        username: String?
    ): Result<Unit> = withContext(GitDispatcher) {
        suspendRunCatching {
            withCredentials(username, tokenOrPassword) { payload ->
                // ls-remote: auth handshake only, no clone, no local repo needed.
                checkGit(gitw_ls_remote(repoUrl, gitCredentialsCallback(), payload)) {}
            }
        }
    }

    override suspend fun initRepository(currentNotebookPath: String): Result<Unit> =
        withContext(GitDispatcher) {
            suspendRunCatching {
                memScoped {
                    val opts = alloc<git_repository_init_options>()
                    checkGit(
                        git_repository_init_options_init(opts.ptr, GIT_REPOSITORY_INIT_OPTIONS_VERSION.convert())
                    ) {}
                    opts.initial_head = "main".cstr.ptr

                    val out = alloc<CPointerVar<git_repository>>()
                    checkGit(git_repository_init_ext(out.ptr, currentNotebookPath, opts.ptr)) {}
                    git_repository_free(out.value)
                }
            }
        }

    override suspend fun cloneRepository(
        repoUrl: String,
        tokenOrPassword: String,
        destinationPath: String,
        username: String?
    ): Result<Unit> = withContext(GitDispatcher) {
        suspendRunCatching {
            withCredentials(username, tokenOrPassword) { payload ->
                checkGit(gitw_clone(repoUrl, destinationPath, gitCredentialsCallback(), payload)) {}
            }
        }
    }

    override suspend fun syncNotes(
        currentNotebookPath: String,
        tokenOrPassword: String,
        username: String?,
        commitMessage: String
    ): Result<GitSyncStatus> = withContext(GitDispatcher) {
        _syncStatus.tryEmit(GitSyncStatus.Syncing)
        suspendRunCatching {
            withRepo(currentNotebookPath) { repo ->
                withCredentials(username, tokenOrPassword) { payload ->
                    val cb = gitCredentialsCallback()

                    // 1. Commit local changes (0 = nothing to do, 1 = committed).
                    checkGit(gitw_commit_if_dirty(repo, commitMessage)) {}

                    // 2. Fetch; a remote with no refs is not an error.
                    checkGit(gitw_fetch(repo, DEFAULT_REMOTE, cb, payload)) {}

                    // 3. Merge upstream; see MergeResult for the codes.
                    when (val merge = gitw_merge_upstream(repo)) {
                        MergeResult.CONFLICT -> GitSyncStatus.Conflict(collectConflicts(repo))
                        MergeResult.UP_TO_DATE, MergeResult.MERGED -> {
                            // 4. Push whatever the remote lacks.
                            pushInternal(repo, cb, payload)
                            GitSyncStatus.UpToDate
                        }
                        // Nothing to pull or push, so the notebook is up to date.
                        MergeResult.NO_UPSTREAM, MergeResult.UNBORN -> GitSyncStatus.UpToDate
                        else -> throw IllegalStateException(
                            "merge failed (status $merge): ${lastGitError()}"
                        )
                    }
                }
            }.also { _syncStatus.tryEmit(it) }
        }.onFailure {
            // Also emit, so observers of gitSyncStatus do not stay stuck on Syncing.
            _syncStatus.tryEmit(GitSyncStatus.GitSyncFailed)
        }
    }

    /** Conflicted paths in the index. */
    private fun collectConflicts(repo: CPointer<git_repository>): Set<String> {
        val paths = mutableSetOf<String>()
        memScoped {
            val out = alloc<CPointerVar<git_index_conflict_iterator>>()
            if (gitw_conflict_iterator_new(out.ptr, repo) < 0) return@memScoped
            val iterator = out.value ?: return@memScoped
            try {
                val buffer = allocArray<ByteVar>(PATH_BUFFER)
                while (gitw_conflict_next_path(iterator, buffer, PATH_BUFFER.convert()) == 0) {
                    paths += buffer.toKString()
                }
            } finally {
                git2.git_index_conflict_iterator_free(iterator)
            }
        }
        return paths
    }

    /** Pushes the current branch; "everything up to date" counts as success. */
    private fun pushInternal(
        repo: CPointer<git_repository>,
        cb: git2.gitw_cred_cb?,
        payload: kotlinx.cinterop.COpaquePointer
    ) {
        val rc = gitw_push(repo, DEFAULT_REMOTE, cb, payload)
        if (rc < 0) {
            // Read the message once: the buffer is invalidated by further calls.
            val message = lastGitError()
            if (!isNothingToPush(message)) throw IllegalStateException(message)
        }
    }

    /** True when a push failed only because there was nothing to send. */
    private fun isNothingToPush(message: String): Boolean =
        message.contains("up-to-date", ignoreCase = true) ||
            message.contains("up to date", ignoreCase = true) ||
            message.contains("src refspec", ignoreCase = true)

    override suspend fun resolveSingleConflict(
        currentNotebookPath: String,
        conflictedFilePath: String,
        keepLocalChanges: Boolean
    ): Result<Unit> = withContext(GitDispatcher) {
        suspendRunCatching {
            withRepo(currentNotebookPath) { repo ->
                // Only the named file; other conflicts stay put.
                resolveConflicts(repo, keepLocalChanges, conflictedFilePath)
                if (gitw_index_conflict_count(repo) == 0) {
                    commitResolvedMerge(repo, "Resolved conflict")
                }
                _syncStatus.tryEmit(GitSyncStatus.UpToDate)
                Unit
            }
        }
    }

    override suspend fun resolveAllConflicts(
        currentNotebookPath: String,
        keepLocalChanges: Boolean
    ): Result<Unit> = withContext(GitDispatcher) {
        suspendRunCatching {
            withRepo(currentNotebookPath) { repo ->
                resolveConflicts(repo, keepLocalChanges, onlyPath = null)
                commitResolvedMerge(repo, "Resolved all conflicts")
                _syncStatus.tryEmit(GitSyncStatus.UpToDate)
                Unit
            }
        }
    }

    /** [onlyPath] null resolves every conflicted file. */
    private fun resolveConflicts(
        repo: CPointer<git_repository>,
        keepLocalChanges: Boolean,
        onlyPath: String?
    ) {
        val rc = if (keepLocalChanges) {
            gitw_resolve_conflicts_ours(repo, onlyPath)
        } else {
            gitw_resolve_conflicts_theirs(repo, onlyPath)
        }
        checkGit(rc) {}
    }

    /** Completes a merge once no conflicts remain. */
    private fun commitResolvedMerge(repo: CPointer<git_repository>, message: String) {
        checkGit(gitw_commit_merge(repo, message)) {}
        git_repository_state_cleanup(repo)
    }

    override suspend fun pushChanges(
        currentNotebookPath: String,
        tokenOrPassword: String,
        username: String?
    ): Result<Unit> = withContext(GitDispatcher) {
        suspendRunCatching {
            withRepo(currentNotebookPath) { repo ->
                withCredentials(username, tokenOrPassword) { payload ->
                    pushInternal(repo, gitCredentialsCallback(), payload)
                }
            }
            _syncStatus.tryEmit(GitSyncStatus.UpToDate)
            Unit
        }
    }

    override suspend fun updateRemoteUrl(
        currentNotebookPath: String,
        newRemoteUrl: String
    ): Result<Unit> = withContext(GitDispatcher) {
        suspendRunCatching {
            withRepo(currentNotebookPath) { repo ->
                val existing = firstRemoteName(repo)
                if (existing.isEmpty()) {
                    // No remote yet: add it and wire the branch upstream.
                    checkGit(gitw_remote_create(repo, DEFAULT_REMOTE, newRemoteUrl)) {}
                    checkGit(gitw_branch_set_upstream(repo, DEFAULT_REMOTE)) {}
                } else {
                    checkGit(gitw_remote_set_url(repo, DEFAULT_REMOTE, newRemoteUrl)) {}
                }
            }
        }
    }

    override suspend fun removeRemote(currentNotebookPath: String): Result<Unit> =
        withContext(GitDispatcher) {
            suspendRunCatching {
                withRepo(currentNotebookPath) { repo ->
                    val existing = firstRemoteName(repo)
                    if (existing.isNotEmpty()) {
                        checkGit(gitw_remote_delete(repo, existing)) {}
                        checkGit(gitw_branch_unset_upstream(repo)) {}
                    }
                }
            }

        }

    /** First configured remote, or "" when there is none. */
    private fun firstRemoteName(repo: CPointer<git_repository>): String = memScoped {
        val buffer = allocArray<ByteVar>(NAME_BUFFER)
        checkGit(gitw_remote_list_first(repo, buffer, NAME_BUFFER.convert())) {}
        buffer.toKString()
    }

    override suspend fun abortMerge(currentNotebookPath: String): Result<Unit> =
        withContext(GitDispatcher) {
            suspendRunCatching {
                withRepo(currentNotebookPath) { repo ->
                    // Only safe mid-merge; otherwise it would discard uncommitted work.
                    if (gitw_merge_in_progress(repo) == 0) return@withRepo

                    checkGit(gitw_reset_hard(repo)) {}
                    git_repository_state_cleanup(repo)
                }
            }
        }

    override fun updateSyncStatus(remoteUrl: String?, syncStatus: GitSyncStatus?): Result<Unit> =
        runCatching {
            _syncStatus.tryEmit(
                syncStatus ?: if (remoteUrl == null) {
                    GitSyncStatus.GitSyncNotConfigured
                } else {
                    GitSyncStatus.UpToDate
                }
            )
            Unit
        }

    private val _syncStatus = MutableSharedFlow<GitSyncStatus>(extraBufferCapacity = 1)
    override val gitSyncStatus: Flow<GitSyncStatus> = _syncStatus.asSharedFlow()
}

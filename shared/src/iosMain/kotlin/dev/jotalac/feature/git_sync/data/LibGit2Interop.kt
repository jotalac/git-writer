@file:OptIn(ExperimentalForeignApi::class)

package dev.jotalac.feature.git_sync.data

import cnames.structs.git_credential
import cnames.structs.git_repository
import git2.git_libgit2_init
import git2.git_repository_free
import git2.git_repository_open
import git2.gitw_cred_userpass
import git2.gitw_error_class
import git2.gitw_error_message
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers

/** Shared plumbing for the libgit2 cinterop binding. */

/** Must run once before any other libgit2 call; lazy, so it costs nothing until used. */
internal object LibGit2 {
    init {
        git_libgit2_init()
    }

    fun ensureInitialised() = Unit
}

/** One lane, because libgit2 forbids concurrent use of the same repository. */
internal val GitDispatcher = Dispatchers.Default.limitedParallelism(1)

/** Merge outcomes returned by `gitw_merge_upstream`. */
internal object MergeResult {
    const val UP_TO_DATE = 0
    const val MERGED = 1
    const val CONFLICT = 2
    const val NO_UPSTREAM = -2
    const val UNBORN = -3
}

/** libgit2's message for the current failure; thread-local, so copy it immediately. */
internal fun lastGitError(): String =
    gitw_error_message()?.toKString()?.takeIf { it.isNotBlank() } ?: "unknown libgit2 error"

/** libgit2 error class (GIT_ERROR_*). */
internal fun lastGitErrorClass(): Int = gitw_error_class()

/** Runs [block], turning a negative libgit2 return into an exception with its message. */
internal inline fun <T> checkGit(rc: Int, block: () -> T): T {
    if (rc < 0) throw IllegalStateException(lastGitError())
    return block()
}

/** Credentials for the C callback; internal because [withCredentials] is inline. */
internal class CredentialState(val username: String?, val password: String)

/** Credentials callback. Cannot capture state, so credentials arrive via the payload. */
private fun credentialCallback(
    out: CPointer<CPointerVar<git_credential>>?,
    url: CPointer<ByteVar>?,
    usernameFromUrl: CPointer<ByteVar>?,
    allowedTypes: UInt,
    payload: COpaquePointer?
): Int {
    if (out == null) return -1
    val state = payload?.asStableRef<CredentialState>()?.get() ?: return -1
    // Blank username means "use the token", matching the JGit implementation.
    val user = state.username?.takeIf { it.isNotBlank() } ?: state.password
    return gitw_cred_userpass(out, user, state.password)
}

/** The C callback to hand to any shim taking credentials. */
internal fun gitCredentialsCallback() = staticCFunction(::credentialCallback)

/** Runs [block] with credentials in a StableRef payload, disposed afterwards. */
internal inline fun <T> withCredentials(
    username: String?,
    tokenOrPassword: String,
    block: (COpaquePointer) -> T
): T {
    val ref = StableRef.create(CredentialState(username, tokenOrPassword))
    try {
        return block(ref.asCPointer())
    } finally {
        ref.dispose()
    }
}

/** Opens a repository and frees it afterwards; the handle cannot escape [block]. */
internal inline fun <T> withRepo(path: String, block: (CPointer<git_repository>) -> T): T = memScoped {
    val out = alloc<CPointerVar<git_repository>>()
    if (git_repository_open(out.ptr, path) < 0) throw IllegalStateException(lastGitError())
    val repo = out.value ?: throw IllegalStateException("libgit2 returned a null repository for $path")
    try {
        block(repo)
    } finally {
        git_repository_free(repo)
    }
}

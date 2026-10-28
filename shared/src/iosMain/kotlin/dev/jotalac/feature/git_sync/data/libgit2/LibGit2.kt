@file:OptIn(ExperimentalForeignApi::class)

package dev.jotalac.feature.git_sync.data.libgit2

import git2.GIT_ENOTFOUND
import git2.git_error_last
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

/*
* The basics every libgit2 call needs: libgit2 reports failures as negative return codes and keeps the
* message for the last one, and it hands out objects through out-parameters that have to be freed.
* */

/** A libgit2 call failed - the message is libgit2's own. */
internal class GitException(message: String, val code: Int = 0) : IllegalStateException(message)

/** Throws a [GitException] when [result] is a libgit2 error code (negative), returns it otherwise. */
internal fun checkGit(result: Int): Int {
    if (result < 0) throw GitException(lastErrorMessage(), code = result)
    return result
}

// per thread, and only meaningful right after the call that failed
private fun lastErrorMessage(): String =
    git_error_last()?.pointed?.message?.toKString()?.takeIf { it.isNotBlank() } ?: "unknown libgit2 error"

/**
 * Calls a libgit2 function that creates an object through an out-parameter, e.g.
 * `create(::git_index_free) { git_repository_index(it, repo) }`. The object is freed with [free] when the
 * memory scope ends, so it must not be used after it.
 */
internal inline fun <T : CPointed> MemScope.create(
    crossinline free: (CPointer<T>) -> Unit,
    call: (CPointer<CPointerVar<T>>) -> Int,
): CPointer<T> = lookup(free) { out -> checkGit(call(out)) } ?: throw GitException("libgit2 created nothing")

/** Like [create], for a lookup of something that may not exist: null instead of an exception then. */
internal inline fun <T : CPointed> MemScope.lookup(
    crossinline free: (CPointer<T>) -> Unit,
    call: (CPointer<CPointerVar<T>>) -> Int,
): CPointer<T>? {
    // a plain pointer slot: libgit2's objects are opaque structs, which cannot be allocated generically
    val out = alloc<COpaquePointerVar>()
    val result = call(interpretCPointer(out.ptr.rawValue)!!)
    if (result == GIT_ENOTFOUND) return null
    checkGit(result)

    val created = interpretCPointer<T>(out.value.rawValue) ?: return null
    defer { free(created) }
    return created
}

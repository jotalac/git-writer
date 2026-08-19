package dev.jotalac.core.utils

import kotlin.coroutines.cancellation.CancellationException

/// custom suspendRunCatching to handle CancellationException
inline fun <T> suspendRunCatching(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
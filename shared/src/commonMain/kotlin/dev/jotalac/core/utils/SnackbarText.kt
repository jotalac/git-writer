package dev.jotalac.core.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Text to show in the UI, resolved against the active locale by [asString]. */
sealed interface SnackbarText {

    /** A string produced outside translation, e.g. a raw exception message. */
    data class Dynamic(val value: String) : SnackbarText

    /**
     * A localized resource, optionally followed by untranslated [detail].
     *
     * [detail] carries the underlying failure text so the cause is not lost when
     * the localized message is generic.
     */
    data class Resource(
        val resource: StringResource,
        val args: List<Any> = emptyList(),
        val detail: String? = null,
    ) : SnackbarText

    companion object {
        /** A localized resource with optional format arguments. */
        fun resource(resource: StringResource, vararg args: Any): Resource =
            Resource(resource, args.toList())

        /** A localized resource followed by the raw [detail], when there is one. */
        fun message(fallback: StringResource, detail: String?): Resource =
            Resource(fallback, detail = detail?.takeIf { it.isNotBlank() })

        /** A raw message that has no resource of its own. */
        fun dynamic(message: String): Dynamic = Dynamic(message)
    }
}

/** Resolves this text for display in the current locale. */
@Composable
fun SnackbarText.asString(): String = when (this) {
    is SnackbarText.Dynamic -> value
    is SnackbarText.Resource -> {
        val localized = if (args.isEmpty()) {
            stringResource(resource)
        } else {
            stringResource(resource, *args.toTypedArray())
        }
        listOfNotNull(localized, detail).joinToString(" ")
    }
}

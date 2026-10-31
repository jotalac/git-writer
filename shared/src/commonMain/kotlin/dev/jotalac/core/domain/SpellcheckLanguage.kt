package dev.jotalac.core.domain

/** The language the editor checks spelling in - or no spell check at all. */
sealed interface SpellcheckLanguage {
    data object SameAsApp : SpellcheckLanguage

    data object Off : SpellcheckLanguage

    data class Of(val language: AppLanguage) : SpellcheckLanguage
}

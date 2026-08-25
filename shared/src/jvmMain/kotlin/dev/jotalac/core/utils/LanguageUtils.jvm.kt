package dev.jotalac.core.utils

import dev.jotalac.core.domain.AppLanguage
import java.util.*

actual fun applyAppLanguage(language: AppLanguage) {
    val locale = Locale.forLanguageTag(language.code)
    Locale.setDefault(locale)
}
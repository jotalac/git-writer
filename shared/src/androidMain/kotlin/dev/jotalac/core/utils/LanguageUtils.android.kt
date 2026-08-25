package dev.jotalac.core.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dev.jotalac.core.domain.AppLanguage
import java.util.*

actual fun applyAppLanguage(language: AppLanguage) {
    val locale = Locale(language.code)
    Locale.setDefault(locale)

    val localeList = LocaleListCompat.forLanguageTags(language.code)
    AppCompatDelegate.setApplicationLocales(localeList)
}
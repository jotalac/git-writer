package dev.jotalac.core.utils

import dev.jotalac.core.domain.AppLanguage
import platform.Foundation.NSUserDefaults

actual fun applyAppLanguage(language: AppLanguage) {
    NSUserDefaults.standardUserDefaults.setObject(
        listOf(language.code),
        forKey = "AppleLanguages"
    )
}
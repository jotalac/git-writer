package dev.jotalac.core.domain

enum class AppLanguage(val code: String, val title: String) {
    ENGLISH("en-US", "English"),
    SPANISH("es-ES", "Español"),
    CZECH("cs-CZ", "Čeština"),
    GERMAN("de-DE", "Deutsch"),

    /** Simplified Chinese, matching the `values-zh` resources. */
    CHINESE("zh-CN", "中文"),
}

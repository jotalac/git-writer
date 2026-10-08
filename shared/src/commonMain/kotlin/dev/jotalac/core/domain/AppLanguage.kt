package dev.jotalac.core.domain

enum class AppLanguage(
    val code: String,
    val title: String,
    val canSpellCheck: Boolean = true,
) {
    ENGLISH("en-US", "English"),
    SPANISH("es-ES", "Español"),
    CZECH("cs-CZ", "Čeština"),
    GERMAN("de-DE", "Deutsch"),
    CHINESE("zh-CN", "中文", canSpellCheck = false),
}

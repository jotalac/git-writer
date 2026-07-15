package dev.jotalac

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
package it.fast4x.riplay

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
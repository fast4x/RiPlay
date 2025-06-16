package it.fast4x.riplay

class Greeting {
    private val platform = it.fast4x.riplay.getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}
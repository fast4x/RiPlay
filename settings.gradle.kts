import org.gradle.kotlin.dsl.maven

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()

        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
        maven( "https://androidx.dev/storage/compose-compiler/repository")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    }
}
/*
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
 */

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
        maven( "https://androidx.dev/storage/compose-compiler/repository")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://maven.mozilla.org/maven2/")
        maven("https://jogamp.org/deployment/maven/") // fallback JogAmp (403 verso Gradle: gli artefatti usati sono 2.6.0 su Maven Central)
    }
}

rootProject.name = "RiPlay"

include(":androidApp")
include(":composeApp")

// Projects from extensions
include(":environment")
project(":environment").projectDir = file("modules/environment")
include(":ktor-client-brotli")
project(":ktor-client-brotli").projectDir = file("modules/ktor-client-brotli")
include(":kugou")
project(":kugou").projectDir = file("modules/kugou")
include(":lrclib")
project(":lrclib").projectDir = file("modules/lrclib")
include(":audiotaginfo")
project(":audiotaginfo").projectDir = file("modules/audiotaginfo")
include(":lastfm")
project(":lastfm").projectDir = file("modules/lastfm")
include(":listenbrainz")
project(":listenbrainz").projectDir = file("modules/listenbrainz")
include(":simpmusiclyrics")
project(":simpmusiclyrics").projectDir = file("modules/simpmusiclyrics")
include(":spotifymeta")
project(":spotifymeta").projectDir = file("modules/spotifymeta")

// Server di streaming locale RiPlay (Relay): modulo JVM puro condiviso da
// androidApp (cast UPnP + VLC/browser) e desktop (VLC/browser/altri RiPlay).
// Engine Ktor CIO, endpoint/whitelist/Range/M3U comuni; le parti di piattaforma
// (sorgente byte, persistenza token) restano in androidApp/server/AndroidRelay.kt
// e composeApp jvmMain/server/DesktopRelay.kt.
include(":relayserver")
project(":relayserver").projectDir = file("modules/relayserver")

// Music Assistant: bridge REST /api/v1 della libreria per il provider Python
// esterno (repo separato riplay-music-assistant). Modulo JVM puro, montato
// NELLO STESSO Ktor application del relay (RelayServer extraModules): stessa
// porta e stesso token del server di streaming, zero porte in piu'.
// Wiring: androidApp/server/BridgeIntegration.kt e
// composeApp jvmMain/server/DesktopBridge.kt.
include(":riplay-bridge")
project(":riplay-bridge").projectDir = file("modules/riplay-bridge")

// ZoundHub: directory open source di segnali AI associati agli artisti
// (https://zoundhub.com - https://github.com/xoundbyte/zoundhub)
include(":zoundhub")
project(":zoundhub").projectDir = file("modules/zoundhub")

//Custom android youtube player
include(":ayp")
project(":ayp").projectDir = file("modules/ayp")
include(":aypui")
project(":aypui").projectDir = file("modules/aypui")
include(":aypcast")
project(":aypcast").projectDir = file("modules/aypcast")

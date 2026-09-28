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
include(":simpmusiclyrics")
project(":simpmusiclyrics").projectDir = file("modules/simpmusiclyrics")
include(":chaquopy")
project(":chaquopy").projectDir = file("modules/chaquopy")
include(":spotifymeta")
project(":spotifymeta").projectDir = file("modules/spotifymeta")

//Custom android youtube player
include(":ayp")
project(":ayp").projectDir = file("modules/ayp")
include(":aypui")
project(":aypui").projectDir = file("modules/aypui")
include(":aypcast")
project(":aypcast").projectDir = file("modules/aypcast")

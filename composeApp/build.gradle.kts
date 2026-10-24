import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.room)
}


fun Project.propertyOrEmpty(name: String): String {
    return (findProperty(name) as? String) ?: ""
}
val generatedSrcDir = layout.buildDirectory.dir("generated/kotlin/config").get()

val generateEnvironmentConfig by tasks.registering {
    group = "build"
    description = "Genera un file Kotlin con le configurazioni di ambiente."

    val environmentPropertyNames = listOf(
        "CrQ0JjAXgv", "hNpBzzAn7i", "lEi9YM74OL", "C0ZR993zmk", "w3TFBFL74Y", "mcchaHCWyK",
        "L2u4JNdp7L", "sqDlfmV4Mt", "WpLlatkrVv", "1zNshDpFoh", "mPVWVuCxJz", "auDsjnylCZ",
        "AW52cvJIJx", "0RGAyC1Zqu", "4Fdmu9Jkax", "kuSdQLhP8I", "QrgDKwvam1", "wLwNESpPtV",
        "JJUQaehRFg", "i7WX2bHV6R", "XpiuASubrV", "lOlIIVw38L", "mtcR0FhFEl", "DTihHAFaBR",
        "a4AcHS8CSg", "krdLqpYLxM", "ye6KGLZL7n", "ec09m20YH5", "LDRlbOvbF1", "EEqX0yizf2",
        "i3BRhLrV1v", "MApdyHLMyJ", "hizI7yLjL4", "rLoZP7BF4c", "nza34sU88C", "dwbUvjWUl3",
        "fqqhBZd0cf", "9sZKrkMg8p", "aQpNCVOe2i", "XNl2TKXLlB", "yNjbjspY8v", "eZueG672lt",
        "WkUFhXtC3G", "z4Xe47r8Vs", "AudioTagInfo_API_KEY", "RiPlay_LASTFM_API_KEY",
        "RiPlay_LASTFM_SECRET", "RiPlay_DISCORD_APPLICATION_ID", "RiPlay_CHROMECAST_APPLICATION_ID"
    )
    inputs.properties(environmentPropertyNames.associateWith { propertyOrEmpty(it) })

    val outputFile = generatedSrcDir.file("it/fast4x/riplay/config/EnvironmentConfig.kt")
    outputs.file(outputFile)

    doLast {
        val file = outputFile.asFile
        file.parentFile.mkdirs()

        val props = inputs.properties

        file.writeText(
            """
            // GENERATED FILE - DO NOT MODIFY
            package it.fast4x.riplay.config

            object EnvironmentConfig {
                const val env_CrQ0JjAXgv = "${props["CrQ0JjAXgv"]}"
                const val env_hNpBzzAn7i = "${props["hNpBzzAn7i"]}"
                const val env_lEi9YM74OL = "${props["lEi9YM74OL"]}"
                const val env_C0ZR993zmk = "${props["C0ZR993zmk"]}"
                const val env_w3TFBFL74Y = "${props["w3TFBFL74Y"]}"
                const val env_mcchaHCWyK = "${props["mcchaHCWyK"]}"
                const val env_L2u4JNdp7L = "${props["L2u4JNdp7L"]}"
                const val env_sqDlfmV4Mt = "${props["sqDlfmV4Mt"]}"
                const val env_WpLlatkrVv = "${props["WpLlatkrVv"]}"
                const val env_1zNshDpFoh = "${props["1zNshDpFoh"]}"
                const val env_mPVWVuCxJz = "${props["mPVWVuCxJz"]}"
                const val env_auDsjnylCZ = "${props["auDsjnylCZ"]}"
                const val env_AW52cvJIJx = "${props["AW52cvJIJx"]}"
                const val env_0RGAyC1Zqu = "${props["0RGAyC1Zqu"]}"
                const val env_4Fdmu9Jkax = "${props["4Fdmu9Jkax"]}"
                const val env_kuSdQLhP8I = "${props["kuSdQLhP8I"]}"
                const val env_QrgDKwvam1 = "${props["QrgDKwvam1"]}"
                const val env_wLwNESpPtV = "${props["wLwNESpPtV"]}"
                const val env_JJUQaehRFg = "${props["JJUQaehRFg"]}"
                const val env_i7WX2bHV6R = "${props["i7WX2bHV6R"]}"
                const val env_XpiuASubrV = "${props["XpiuASubrV"]}"
                const val env_lOlIIVw38L = "${props["lOlIIVw38L"]}"
                const val env_mtcR0FhFEl = "${props["mtcR0FhFEl"]}"
                const val env_DTihHAFaBR = "${props["DTihHAFaBR"]}"
                const val env_a4AcHS8CSg = "${props["a4AcHS8CSg"]}"
                const val env_krdLqpYLxM = "${props["krdLqpYLxM"]}"
                const val env_ye6KGLZL7n = "${props["ye6KGLZL7n"]}"
                const val env_ec09m20YH5 = "${props["ec09m20YH5"]}"
                const val env_LDRlbOvbF1 = "${props["LDRlbOvbF1"]}"
                const val env_EEqX0yizf2 = "${props["EEqX0yizf2"]}"
                const val env_i3BRhLrV1v = "${props["i3BRhLrV1v"]}"
                const val env_MApdyHLMyJ = "${props["MApdyHLMyJ"]}"
                const val env_hizI7yLjL4 = "${props["hizI7yLjL4"]}"
                const val env_rLoZP7BF4c = "${props["rLoZP7BF4c"]}"
                const val env_nza34sU88C = "${props["nza34sU88C"]}"
                const val env_dwbUvjWUl3 = "${props["dwbUvjWUl3"]}"
                const val env_fqqhBZd0cf = "${props["fqqhBZd0cf"]}"
                const val env_9sZKrkMg8p = "${props["9sZKrkMg8p"]}"
                const val env_aQpNCVOe2i = "${props["aQpNCVOe2i"]}"
                const val env_XNl2TKXLlB = "" //"${props["XNl2TKXLlB"]}"
                const val env_yNjbjspY8v = "" //"${props["yNjbjspY8v"]}"
                const val env_eZueG672lt = "${props["eZueG672lt"]}"
                const val env_WkUFhXtC3G = "${props["WkUFhXtC3G"]}"
                const val env_z4Xe47r8Vs = "${props["z4Xe47r8Vs"]}"
                const val AudioTagInfo_API_KEY = "${props["AudioTagInfo_API_KEY"]}"
                const val RiPlay_LASTFM_API_KEY = "${props["RiPlay_LASTFM_API_KEY"]}"
                const val RiPlay_LASTFM_SECRET = "${props["RiPlay_LASTFM_SECRET"]}"
                const val RiPlay_DISCORD_APPLICATION_ID = "${props["RiPlay_DISCORD_APPLICATION_ID"]}"
                const val RiPlay_CHROMECAST_APPLICATION_ID = "${props["RiPlay_CHROMECAST_APPLICATION_ID"]}"
            }
            """.trimIndent()
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateEnvironmentConfig)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

compose.desktop {
    application {

        mainClass = "MainKt"

        version = "0.0.1"
        group = "riplay"

        nativeDistributions {
            vendor = "RiPlay.DesktopApp"
            description = "RiPlay Desktop Music Player"

            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "RiPLay.DesktopApp"
            packageVersion = "0.0.1"

            /*
            val iconsRoot = project.file("desktop-icons")
            windows {
                iconFile.set(iconsRoot.resolve("icon-windows.ico"))
            }
            macOS {
                iconFile.set(iconsRoot.resolve("icon-mac.icns"))
            }
            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
            }

             */
        }

    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}

kotlin {
    androidLibrary {
        namespace = "it.fast4x.riplay.composeapp"
        compileSdk = 37
        minSdk = 23
    }

    jvm()

    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }

        val commonMain by getting {
            kotlin.srcDir(generatedSrcDir)

            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ktor.client.websockets)

                implementation(projects.environment)
                implementation(projects.kugou)
                implementation(projects.lrclib)
                implementation(projects.audiotaginfo)
                implementation(projects.lastfm)

                implementation(libs.room.runtime)
                implementation(libs.room.sqlite.bundled)

                implementation(libs.mediaplayer.kmp)

                implementation(libs.navigation.kmp)

                //coil3 mp
                implementation(libs.coil.compose.core)
                implementation(libs.coil.compose)
                implementation(libs.coil.mp)

                implementation(libs.translator)
                implementation(libs.reorderable)

                implementation(libs.fastscroller)
                implementation(libs.fastscroller.material3)
                implementation(libs.fastscroller.indicator)

            }
        }

        val jvmMain by getting
        jvmMain.dependencies {
            implementation(compose.components.resources)
            implementation(compose.desktop.currentOs)

            implementation(libs.material.icon.desktop)
            implementation(libs.vlcj)
            implementation(libs.kcef)

            // JOGL/GlueGen runtime per KCEF (motore YouTube desktop)
            // jogamp.org risponde 403 al client HTTP di Gradle: si usano gli artefatti 2.6.0 su Maven Central
            // (jogl-all.jar include le classi nativewindow/newt; la 2.6.0 vince sulla 2.5.0 transitiva di kcef)
            runtimeOnly("org.jogamp.gluegen:gluegen-rt:2.6.0")
            runtimeOnly("org.jogamp.jogl:jogl-all:2.6.0")
            runtimeOnly("org.jogamp.gluegen:gluegen-rt:2.6.0:natives-windows-amd64")
            runtimeOnly("org.jogamp.gluegen:gluegen-rt:2.6.0:natives-linux-amd64")
            runtimeOnly("org.jogamp.gluegen:gluegen-rt:2.6.0:natives-macosx-universal")
            runtimeOnly("org.jogamp.jogl:jogl-all:2.6.0:natives-windows-amd64")
            runtimeOnly("org.jogamp.jogl:jogl-all:2.6.0:natives-linux-amd64")
            runtimeOnly("org.jogamp.jogl:jogl-all:2.6.0:natives-macosx-universal")

            val fxSuffix = "win"
            implementation("org.openjfx:javafx-base:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-graphics:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-controls:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-swing:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-web:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-media:21.0.5:${fxSuffix}")

            implementation(libs.coil.network.okhttp)
            runtimeOnly(libs.kotlinx.coroutines.swing)

        }

        androidMain.dependencies {
            implementation(libs.room.ktx)
            implementation(
                fileTree(
                    mapOf(
                        "dir" to "libs",
                        "include" to listOf("*.aar", "*.jar")
                    )
                )
            )

        }

    }
}


room {
    schemaDirectory("$projectDir/schemas")
    generateKotlin = true
}


dependencies {
    // Room compiler per la @Database fusa in commonMain (DatabaseInitializer v68).
    // Prima della fusione era dichiarato solo in androidApp; il Gradle plugin Room
    // gestisce gli schemi ma NON aggiunge il compiler -> senza questa riga l'APK
    // esce senza DatabaseInitializer_Impl e crasha al primo accesso al DB.
    // NOTA: la config ksp() top-level e' vietata in KMP -> servono config per-target.
    add("kspJvm", libs.room.compiler)
    add("kspAndroid", libs.room.compiler)
    // NOTA: compilazioni jvm "dev"/"test" (hot-reload/unit test) NON sono coperte
    // (config KSP non esposte da questa versione plugin); il run desktop normale usa kspJvm
}

// KSP dipende dal config generato di environment (output condiviso build/generated/kotlin/config)
afterEvaluate {
    tasks.matching { it.name.startsWith("ksp") }.configureEach {
        dependsOn(tasks.matching { it.name == "generateEnvironmentConfig" })
    }
}

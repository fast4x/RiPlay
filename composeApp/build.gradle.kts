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

        // STEP 3q: versione allineata all'app Android (versionName)
        version = "0.8.1"
        group = "riplay"

        nativeDistributions {
            vendor = "RiPlay.DesktopApp"
            description = "RiPlay Desktop Music Player"
            copyright = "Copyright (c) RiPlay.DesktopApp"
            // Licenza inclusa nel pacchetto (file LICENSE alla radice repo)
            licenseFile.set(rootProject.file("LICENSE"))

            /*
             * STEP 4 - PACCHETTIZZAZIONE DEB/MSI/DMG (richiesta utente):
             *  - Msi si costruisce SOLO su Windows, Deb/Rpm SOLO su Linux,
             *    Dmg SOLO su macOS (jpackage non fa cross-building): ogni
             *    formato viene prodotto dal runner corrispondente nel
             *    workflow GitHub Actions unificato
             *    (.github/workflows/build-release.yml, job build-desktop).
             *  - CEF PRE-SEMINATO: gli archivi jbr_jcef (selezionati come fa
             *    KCEF, v. scripts/fetch-kcef-bundle.sh) vanno messi in
             *    composeApp/desktop-resources/<os>/kcef/ (la DSL copia nell'app
             *    solo le sottocartelle per sistema: linux | windows | macos)
             *    PRIMA della build: finiscono nell'applicazione (resources/kcef) e
             *    KcefEngine li estrae
             *    al primo avvio nella directory dati utente (nessun download
             *    ~200MB per l'utente finale). Se l'archivio manca, l'app
             *    continua a scaricare il bundle come sempre (fallback).
             *  - VLC (motore Locale): resta un REQUISITO DI SISTEMA (vlcj
             *    NativeDiscovery trova libvlc di VLC installato).
             */
            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Dmg)
            packageName = "RiPlay"
            packageVersion = "0.8.1"

            // STEP 4: risorse applicative incluse nel pacchetto (CEF pre-seminato)
            appResourcesRootDir.set(project.layout.projectDirectory.dir("desktop-resources"))

            // STEP 4: runtime completo (ktor/room/serialization/coroutines usano
            // reflection e moduli java che il default di jpackage esclude)
            includeAllModules = true

            val iconsRoot = project.file("desktop-icons")

            windows {
                iconFile.set(iconsRoot.resolve("icon-windows.ico"))
                menu = true
                shortcut = true
                dirChooser = true
                perUserInstall = true
                // UUID fisso: le versioni successive si aggiornano correttamente
                upgradeUuid = "54b878d7-1cb9-47b7-b948-4f9308ae2e86"
            }
            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
                menuGroup = "Audio;AudioVideo"
                // NOTA: sostituire con l'email reale del manutentore del deb
                debMaintainer = "riplay.desktopapp@gmail.com"
            }
            macOS {
                iconFile.set(iconsRoot.resolve("icon-mac.icns"))
                bundleID = "it.fast4x.riplay.desktop"
                minimumSystemVersion = "10.15"
            }
        }

    }
}

// =============================================================================
// STEP 3u (fix build DMG su CI macOS):
// In fase app-image jpackage su macOS esegue, per OGNI file eseguibile o
// .dylib presente in RiPlay.app, "codesign --remove-signature" + "codesign
// -s -" (la firma ad-hoc e' obbligatoria su Apple Silicon): se anche UN SOLO
// binario e' unsigned la build muore con "External tool execution failed ...
// Exit code: 1" e l'errore reale appare SOLO con --verbose (v. issue
// JetBrains/compose-multiplatform#3586, stessa identica firma).
// Il plugin Compose pre-firma le dylib estratte dai jar, ma il runtime
// prodotto da jlink (task createRuntimeImage) e' l'unica parte che NON viene
// pre-firmata: qui lo firmiamo ad-hoc ("-" = firma senza certificato,
// sufficiente per eseguire su Apple Silicon) PRIMA che jpackage parta, cosi'
// la fase di firma di jpackage non trova mai sottocomponenti unsigned.
// Su Windows/Linux e' un no-op (codesign non esiste). Un eventuale errore
// codesign e' SOLO un warning: jpackage gestisce comunque la firma.
// =============================================================================
tasks.matching { it.name == "createRuntimeImage" }.configureEach {
    doLast {
        val osName = System.getProperty("os.name").lowercase()
        if (!osName.contains("mac") && !osName.contains("darwin")) return@doLast
        val runtimeDir = outputs.files.firstOrNull { it.isDirectory && it.exists() } ?: return@doLast
        logger.lifecycle("[riplay-mac] pre-firma ad-hoc del runtime jlink: ${runtimeDir.absolutePath}")
        var signedOk = 0
        var signedKo = 0
        runtimeDir.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val isLib = file.extension == "dylib" || file.name.endsWith(".jnilib")
                val isExec = file.canExecute()
                if (!isLib && !isExec) return@forEach
                val proc = ProcessBuilder(
                    "/usr/bin/codesign", "--force", "--sign", "-", file.absolutePath
                ).redirectErrorStream(true).start()
                val outputMsg = proc.inputStream.bufferedReader().readText().trim()
                val exitCode = proc.waitFor()
                if (exitCode == 0) {
                    signedOk++
                } else {
                    signedKo++
                    logger.warn("[riplay-mac] codesign fallito su ${file.name}: $outputMsg")
                }
            }
        logger.lifecycle("[riplay-mac] runtime pre-firmato: $signedOk file firmati, $signedKo errori")
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

            // STEP 4 (build Mac/Linux): il classifier JavaFX segue il SISTEMA
            // CHE COSTRUISCE (prima era "win" fisso: giusto solo per la build
            // locale Windows dell'utente). Su Apple Silicon = mac-aarch64.
            // JavaFX non e' importato dal codice desktop (resta nel classpath
            // per compatibilita' con librerie che lo toccano a runtime).
            val osName = System.getProperty("os.name").lowercase()
            val osArch = System.getProperty("os.arch").lowercase()
            val fxSuffix = when {
                osName.contains("win") -> "win"
                osName.contains("mac") || osName.contains("darwin") ->
                    if (osArch.contains("aarch64") || osArch.contains("arm64")) "mac-aarch64" else "mac"
                else -> "linux"
            }
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

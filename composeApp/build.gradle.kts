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

// =============================================================================
// STEP 3v (fix DMG macOS - createDistributable fallisce con jpackage exit 1):
//   jdk.jpackage.internal.ConfigException: The first number in an app-version
//   cannot be zero or negative (jdk.jpackage.internal.CFBundleVersion.of,
//   MacAppBundler.doValidate -> AppImageBundler.validate)
// CAUSA: su macOS jpackage valida la CFBundleVersion del bundle e il PRIMO
// numero dell'app-version NON puo' essere 0; Windows (Msi) e Linux (Deb/Rpm)
// invece accettano 0.x. La versione desktop e' allineata all'app Android
// ("0.8.0", schema 0.x), quindi per macOS la versione viene mappata in modo
// deterministico: se il primo numero e' 0 viene promosso a 1
// ("0.8.0" -> "1.8.0"), mantenendo minor/patch riconoscibili.
// (verificato sul sorgente del plugin Compose 1.11.0:
//  - packageVersionFor(AppImage/Dmg) risolve macOS.packageVersion PRIMA del
//    top-level packageVersion (internal/packageVersions.kt)
//  - CFBundleVersion = packageBuildVersion ?: packageVersion
//    (AbstractJPackageTask.patchInfoPlistIfNeeded))
// Cosi' il tag GitHub v0.8.0 resta invariato e i prossimi 0.x funzioneranno
// automaticamente; i tag v1.x passano la funzione senza modifiche.
// =============================================================================
fun macSafeJPackageVersion(raw: String): String {
    val nums = raw.split('.').map { part ->
        part.trim().takeWhile { it.isDigit() }.ifEmpty { "0" }.toInt()
    }
    if (nums.isEmpty()) return "1.0.0"
    val first = if (nums[0] <= 0) 1 else nums[0]
    val rest = List(2) { idx -> nums.getOrNull(idx + 1) ?: 0 }
    return (listOf(first) + rest).joinToString(".")
}

// =============================================================================
// STEP 3ah (release candidate): versioning desktop con suffisso pre-release
// La versione desktop puo' portare un suffisso SemVer 2.0.0 ("0.8.7-rc.1",
// "0.8.7-beta.2", ...): la GitHub Release viene marcata "pre-release" dal
// workflow (STEP 3ah di build-release.yml). jpackage pero' NON accetta
// suffissi: --app-version deve essere "1-3 interi separati da punti" per
// QUALUNQUE formato (Msi/Deb/Rpm/Dmg/AppImage), e RPM rifiuta anche il
// trattino. Quindi la versione viaggia in TRE forme:
//   - riplayDesktopVersion        = versione COMPLETA (anche con suffisso):
//     resta sui metadati applicazione (version =) e sul log diagnostico
//   - riplayDesktopVersionNumeric = parte numerica (taglio al primo "-"):
//     e' quella passata a jpackage su Windows/Linux
//   - riplayDesktopVersionMac     = mac-safe della numerica (STEP 3v:
//     "0.8.7" -> "1.8.7"): quella passata a jpackage su macOS
// Fail-fast: se la parte numerica non e' 1-3 interi la CONFIGURAZIONE
// gradle fallisce subito con messaggio esplicito (mai ConfigException
// oscura di jpackage a meta' build CI).
// =============================================================================
fun numericPackagingVersion(raw: String): String {
    val core = raw.substringBefore('-').trim()
    val parts = core.split('.').map { it.trim() }
    require(parts.size in 1..3 && parts.all { it.isNotEmpty() && it.all(Char::isDigit) }) {
        "Versione desktop non valida: '$raw' (parte numerica '$core'). " +
            "Formato atteso: 1-3 interi separati da punti, con eventuale suffisso " +
            "pre-release dopo '-' (es. 0.8.7, 0.8.7-rc.1, 1.0.0-beta.2)."
    }
    return core
}

val riplayDesktopVersion = "0.8.7-rc.1"
// STEP 3ah: parte numerica per jpackage: "0.8.7-rc.1" -> "0.8.7"
val riplayDesktopVersionNumeric = numericPackagingVersion(riplayDesktopVersion)
// STEP 3v: su macOS jpackage rifiuta 0.x -> la NUMERICA viene promossa
// ("0.8.7" -> "1.8.7"); il suffisso pre-release e' gia' stato rimosso
val riplayDesktopVersionMac = macSafeJPackageVersion(riplayDesktopVersionNumeric)

// =============================================================================
// STEP 3x (difesa finale app-version macOS + canarino diagnostico):
// Verifica sui sorgenti plugin Compose 1.11.0 (internal/packageVersions.kt +
// dsl/TargetFormat.kt): packageVersionFor(AppImage) -> formatSpecific (null
// per AppImage) ?: osSpecific (macOS.packageVersion, perche' AppImage ha
// targetOS = currentOS che su runner macOS e' MacOS) ?: top-level. Il fix
// STEP 3v nel blocco macOS{} e' quindi corretto; il fallimento CI del
// 27/10 08:28Z con lo stesso errore e' imputabile a una run che NON
// conteneva questo codice (tag su commit vecchio / checkout su ref diverso:
// il workflow su workflow_dispatch esegue anche "git reset --hard origin/main").
// Difese aggiuntive:
// 1) versione mac-safe impostata ANCHE sul packageVersion top-level quando
//    il build gira su host macOS: qualunque formato/target riceve 1.8.0
//    indipendentemente dalla catena di risoluzione (su Linux/Windows nulla
//    cambia: resta 0.8.0)
// 2) la riga "RiPlay packaging version:" nel log di configurazione rende
//    visibile in OGNI run quale codice sta girando: se una run fallisce con
//    "The first number in an app-version" SENZA questa riga, la run NON
//    contiene questo file (controllare tag/branch del push).
//    NB: println = livello QUIET, visibile anche con org.gradle.logging.level=warn
//    (logger.lifecycle verrebbe filtrato dal log level warn di gradle.properties)
// =============================================================================
val riplayMacHost = (System.getProperty("os.name") ?: "").startsWith("Mac", ignoreCase = true)
// STEP 3ah: jpackage riceve SEMPRE una versione numerica (numeric o macSafe),
// mai la versione completa con suffisso pre-release
val riplayPackageVersionForHost = if (riplayMacHost) riplayDesktopVersionMac else riplayDesktopVersionNumeric
println(
    "RiPlay packaging version: host='" + System.getProperty("os.name") + "' " +
        "packageVersion=$riplayPackageVersionForHost (raw=$riplayDesktopVersion, numeric=$riplayDesktopVersionNumeric, macSafe=$riplayDesktopVersionMac)"
)

compose.desktop {
    application {

        mainClass = "MainKt"

        // STEP 3q: versione allineata all'app Android (versionName)
        // STEP 3ah: qui resta la versione COMPLETA (anche con suffisso
        // "-rc.1"): e' solo metadato, jpackage NON la vede mai (i package*
        // Version qui sotto usano sempre la parte numerica o la mac-safe)
        // NOTA: dentro application{} "version =" risolve sulla proprieta' del
        // Project Gradle (receiver esterno al lambda), il plugin Compose 1.11
        // non espone una version propria su JvmApplication
        version = riplayDesktopVersion
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
             *  - CEF PRE-SEMINATO (v46, jcefmaven): l'archivio natives
             *    "jcef-natives-<platform>-<tag>.tar.gz" (stesso formato che
             *    jcefmaven estrae: v. composeApp/desktop-resources/README.md)
             *    va messo in composeApp/desktop-resources/<os>/kcef/ (la DSL
             *    copia nell'app solo le sottocartelle per sistema:
             *    linux | windows | macos) PRIMA della build: finisce
             *    nell'applicazione (resources/kcef) e KcefEngine lo estrae
             *    al primo avvio nella directory dati utente (nessun download
             *    ~150MB per l'utente finale). Se l'archivio manca o ha un tag
             *    diverso dalla versione CEF dell'app, jcefmaven scarica
             *    automaticamente il bundle corretto (fallback, come sempre).
             *  - VLC (motore Locale): resta un REQUISITO DI SISTEMA (vlcj
             *    NativeDiscovery trova libvlc di VLC installato).
             */
            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Dmg)
            packageName = "RiPlay"
            // STEP 3x: su host macOS qui arriva gia' la versione mac-safe,
            // cosi' jpackage non puo' mai ricevere 0.x per qualunque formato
            // (createDistributable/AppImage incluso).
            // STEP 3ah: su Linux/Windows arriva la parte NUMERICA della
            // versione (es. "0.8.7-rc.1" -> "0.8.7"): jpackage rifiuta
            // qualunque suffisso su Msi/Deb/Rpm (e RPM rifiuta il trattino)
            packageVersion = riplayPackageVersionForHost

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
                // STEP 3v: su macOS jpackage rifiuta versioni 0.x (CFBundleVersion
                // con primo numero 0 o negativo). "0.8.0" -> "1.8.0": vale per
                // createDistributable (--app-version), packageDmg e la
                // CFBundleVersion/CFBundleShortVersionString dell'Info.plist.
                // Windows/Linux continuano a usare la numerica (top-level).
                // STEP 3ah: qui arriva la mac-safe della parte NUMERICA
                // ("0.8.7-rc.1" -> numerica "0.8.7" -> mac-safe "1.8.7"):
                // il suffisso pre-release e' gia' stato rimosso
                packageVersion = riplayDesktopVersionMac
                packageBuildVersion = riplayDesktopVersionMac
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

            // v46 (jcefmaven): motore CEF = jcefmaven (me.friwi) mantenuto attivo
            // ( ultima release 152.0.6, Chromium 152 ) in sostituzione di KCEF
            // (dev.datlag:kcef, ultima release 2025.03.23, non piu' aggiornato).
            // jcefmaven porta transitivamente jcef-api (classi org.cef) e le sue
            // dipendenze jogl (me.friwi:jogl-all/gluegen-rt, non usate in rendering
            // windowed). Le natives CEF vengono estratte dal bundle pre-seminato
            // nell'app o scaricate al primo avvio (v. KcefEngine).
            implementation(libs.jcefmaven)

            implementation(libs.coil.network.okhttp)
            implementation(projects.simpmusiclyrics)
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

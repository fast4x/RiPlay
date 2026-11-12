import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.zip.ZipFile
import javax.imageio.ImageIO

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

val riplayDesktopVersion = "0.8.7-rc.2"
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

// =============================================================================
// STEP 4b (branding degli installer): valori condivisi tra la DSL
// nativeDistributions{} qui sotto e i task jpackage "branded" definiti al
// STEP 4c: un'unica fonte di verita' per nome, venditore, descrizione,
// impostazioni Windows e bundleID macOS, cosi' gli installer generati dai
// task packageBranded* restano sempre allineati alla configurazione jpackage.
// =============================================================================
val riplayPackageName = "RiPlay"
val riplayVendor = "RiPlay.DesktopApp"
val riplayDescription = "RiPlay Desktop Music Player"
val riplayCopyright = "Copyright (c) RiPlay.DesktopApp"
// Impostazioni Windows (usate sia dalla DSL windows{} sia dai task branded)
val riplayWinMenu = true
val riplayWinShortcut = true
val riplayWinDirChooser = true
val riplayWinPerUserInstall = true
val riplayWinUpgradeUuid = "54b878d7-1cb9-47b7-b948-4f9308ae2e86"
// Bundle ID macOS (usato sia dalla DSL macOS{} sia dai task branded DMG)
val riplayMacBundleId = "it.fast4x.riplay.desktop"

compose.desktop {
    application {

        // STEP 4d: ProGuard disattivato per il build type release.
        // Il plugin Compose recente abilita ProGuard DI DEFAULT su release:
        // proguardReleaseJars fallisce con "Please correct the above warnings
        // first" (log 12/11: 658 classi non risolte + 31 membri, tutte
        // dipendenze opzionali incluse nei jar ma non usate sul desktop:
        // SWT di jogamp, GraalVM/okhttp, conscrypt/bouncycastle/openjsse,
        // pack200/ASM, OpenAL, interni JDK mac di org.cef, ...). Con tanta
        // reflection in gioco (ktor/room/serialization/vlcj/jcef) la
        // minificazione rompe piu' di quanto compatti: si torna al
        // comportamento delle versioni precedenti del plugin (release senza
        // ProGuard). Per riattivarla in futuro: sostituire questo blocco con
        //   buildTypes.release.proguard {
        //       isEnabled.set(true)
        //       configurationFiles.from("desktop-proguard-rules.pro")
        //   }
        // e usare il file desktop-proguard-rules.pro consegnato a parte
        // (punto di partenza, DA TESTARE a runtime).
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

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
            vendor = riplayVendor
            description = riplayDescription
            copyright = riplayCopyright
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
            packageName = riplayPackageName
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
                menu = riplayWinMenu
                shortcut = riplayWinShortcut
                dirChooser = riplayWinDirChooser
                perUserInstall = riplayWinPerUserInstall
                // UUID fisso: le versioni successive si aggiornano correttamente
                upgradeUuid = riplayWinUpgradeUuid
            }
            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
                menuGroup = "Audio;AudioVideo"
                // NOTA: sostituire con l'email reale del manutentore del deb
                debMaintainer = "riplay.desktopapp@gmail.com"
            }
            macOS {
                iconFile.set(iconsRoot.resolve("icon-mac.icns"))
                bundleID = riplayMacBundleId
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

// =============================================================================
// STEP 4b (branding degli installer + avvio a fine installazione - richieste
// utente):
//   1) LOGO DELL'APP NEL SETUP DI INSTALLAZIONE
//   2) AVVIO DI RiPlay ALLA FINE DELL'INSTALLAZIONE, SE L'UTENTE LO DESIDERA
// Situazione per formato (jpackage non fa cross-building: v. STEP 4):
//  - Windows Msi: e' l'UNICO installer con un wizard grafico. Il wizard nasce
//    dal template WiX "main.wxs" interno a jpackage
//    (jdk/jpackage/internal/resources/main.wxs), che referenzia la dialog set
//    WixUI_InstallDir di WixUIExtension (jpackage genera anche ui.wxf con
//    <UIRef Id="WixUI_InstallDir"/> e i Publish tra le finestre):
//      * immagini del wizard = WixVariable standard:
//          WixUIDialogBmp 493x312 (finestre di benvenuto/fine, resa 370x234)
//          WixUIBannerBmp 493x58  (banner delle finestre interne)
//      * avvio a fine installazione = ricetta WiX standard:
//          - Property WIXUI_EXITDIALOGOPTIONALCHECKBOXTEXT: rende visibile la
//            checkbox opzionale gia' presente nella ExitDialog di WixUIExtension;
//          - CustomAction WixShellExec (BinaryKey="WixCA", fornito da
//            WixUIExtension, che jpackage linka sempre che ci sia la UI), con
//            target impostato da una CustomAction Type 51 (valore Formatted):
//            WixShellExecTarget = "[INSTALLDIR]RiPlay.exe" (tra virgolette
//            gestisce percorsi con spazi; installazione per-utente, nessun id
//            File richiesto: l'id del launcher in bundle.wxf e' un UUID
//            derivato dal percorso, non stabile);
//          - <Publish Dialog="ExitDialog" Control="Finish">: lancia l'app
//            SOLO se la checkbox e' spuntata (WIXUI_EXITDIALOGOPTIONALCHECKBOX
//            = 1) e solo in installazione fresca (NOT Installed: mai su upgrade).
//  - macOS Dmg: il "setup" e' la finestra di drag&drop del DMG. jpackage
//    deposita lo sfondo in "<DMG>/.background/background.tiff" e lo script di
//    setup di default (DMGsetup.scpt: finestra 520x340, icona app a {120,130},
//    alias Applicazioni a {390,130}) lo imposta GIA' come sfondo: basta
//    fornire la risorsa "background_dmg.tiff" (520x340 px) nella resource dir
//    (MacDmgBundler: DEFAULT_BACKGROUND_IMAGE = "background_dmg.tiff").
//  - Linux Deb/Rpm: l'installazione passa dai package manager (apt/dnf), che
//    non hanno wizard grafico personalizzabile: nessun logo inseribile e
//    nessun momento in cui proporre l'avvio dell'app. L'icona nel menu/desktop
//    resta curata da iconFile + menuGroup (invariati).
// IMPLEMENTAZIONE: il plugin Compose 1.11 imposta la --resource-dir di
// jpackage su build/compose/tmp/resources e la SVUOTA all'inizio di ogni task
// di packaging (AbstractJPackageTask.prepareWorkingDir), senza punti di
// aggancio intermedi; "freeArgs" non puo' sostituirla (arriva PRIMA degli
// argomenti del plugin e in jpackage l'ultimo --resource-dir vince). Quindi:
//   - i task standard packageMsi/packageDmg (+ varianti Release) vengono
//     disattivati e sostituiti dai task "branded" (STEP 4c sotto), che
//     eseguono jpackage in modalita' --app-image sull'app image prodotta da
//     createDistributable/createReleaseDistributable (stessa catena di sempre:
//     jlink, codici versione STEP 3x/3ah, codesign STEP 3u) aggiungendo la
//     --resource-dir di branding generata qui;
//   - il template main.wxs NON viene copiato a mano nel repo: viene ESTRATTO a
//     build time dal JDK che esegue jpackage (jmod, poi filesystem jrt) e il
//     branding viene iniettato prima di </Product> => resta corretto con
//     qualunque versione/aggiornamento del JDK;
//   - gli aggregatori packageDistributionForCurrentOS e
//     packageReleaseDistributionForCurrentOS (chiamati dal workflow CI) sono
//     ricondotti ai task branded: il workflow CI NON va modificato.
// Immagini: generazione automatica con layout studiato sui controlli WiX
// FISSI di WixUI_InstallDir (v. commento su riplayBrandCanvas):
//   - dialog.bmp 493x312: logo SOLIDO a sinistra del testo (colonna x 30..170,
//     centrato in verticale); il testo del wizard parte da ~x 180 px;
//   - banner.bmp 493x58: WATERMARK leggero (alpha 18%) a destra, perche' il
//     testo WiX e' disegnato sopra il banner per quasi tutta la larghezza.
// Per usare immagini dedicate basta aggiungere in composeApp/desktop-icons/:
//     logo.png            logo sorgente (default: icon-linux.png; preferibile
//                         un PNG con trasparenza)
//     msi-dialog.bmp      493x312 px (sostituisce quella generata)
//     msi-banner.bmp      493x58  px (sostituisce quella generata)
//     dmg-background.png  520x340 px (convertita in TIFF per il DMG)
// Leva di emergenza: eseguire con -PriplayInstallerBranding=false per
// ripristinare i task Msi/Dmg standard SENZA branding (es. per confronti).
// =============================================================================

val riplayWindowsHost = (System.getProperty("os.name") ?: "").lowercase().contains("win")
// Leva di emergenza (vedi commento STEP 4b)
val riplayInstallerBrandingEnabled =
    ((findProperty("riplayInstallerBranding") as? String) ?: "true").lowercase() != "false"

val riplayDesktopIconsDir = project.file("desktop-icons")
// Logo sorgente (PNG) per le immagini derivate degli installer
val riplayLogoPng = sequenceOf(
    riplayDesktopIconsDir.resolve("logo.png"),
    riplayDesktopIconsDir.resolve("icon-linux.png")
).firstOrNull { it.isFile } ?: riplayDesktopIconsDir.resolve("icon-linux.png")
// Sostituti opzionali forniti a mano (vedi tabella nel commento STEP 4b)
val riplayCustomMsiDialogBmp = riplayDesktopIconsDir.resolve("msi-dialog.bmp")
val riplayCustomMsiBannerBmp = riplayDesktopIconsDir.resolve("msi-banner.bmp")
val riplayCustomDmgBackgroundPng = riplayDesktopIconsDir.resolve("dmg-background.png")

val riplayMsiResourcesDir = layout.buildDirectory.dir("riplay/installer-resources/msi")
val riplayDmgResourcesDir = layout.buildDirectory.dir("riplay/installer-resources/dmg")

// Testo della checkbox nella pagina finale del wizard MSI (richiesta 2)
val riplayLaunchCheckboxText = "Avvia RiPlay al termine dell'installazione"

// Disegna il logo (aspect-fit dentro un box, con alpha opzionale) su una tela
// RGB bianca: sorgente per i BMP del wizard MSI e per lo sfondo TIFF del DMG.
// Il layout tiene conto dei controlli WiX FISSI di WixUI_InstallDir (le dialog
// stanno in WixUIExtension.dll e NON si possono spostare via resource-dir):
//   - benvenuto/fine (WelcomeDlg/ExitDialog/UserExit, bitmap 493x312): titolo,
//     descrizione e checkbox sono nella colonna che parte da X=135
//     dialog-unit = ~180 px => logo SOLIDO a sinistra del testo (colonna
//     x 30..170), il testo resta su fondo bianco perfettamente leggibile;
//   - pagine con banner (licenza, percorso, conferma, progresso, 493x58):
//     titolo a X=15/Y=6 e descrizione a X=25/Y=23 dialog-unit disegnati SOPRA
//     il banner per quasi tutta la larghezza (la descrizione della licenza
//     arriva fino a ~486 px) => nessuna zona solida libera: logo come
//     WATERMARK leggero a destra (stesso approccio dell'arte di default WiX,
//     testo nero sempre leggibile sopra).
fun riplayBrandCanvas(
    logo: BufferedImage, width: Int, height: Int,
    boxX: Int, boxY: Int, boxW: Int, boxH: Int, alphaPercent: Int
): BufferedImage {
    val canvas = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = canvas.createGraphics()
    try {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.color = Color.WHITE
        g.fillRect(0, 0, width, height)
        val scale = minOf(
            boxW.toDouble() / logo.width.toDouble(),
            boxH.toDouble() / logo.height.toDouble()
        )
        val drawW = (logo.width * scale).toInt().coerceAtLeast(1)
        val drawH = (logo.height * scale).toInt().coerceAtLeast(1)
        val drawX = boxX + (boxW - drawW) / 2
        val drawY = boxY + (boxH - drawH) / 2
        if (alphaPercent in 1..99) {
            g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaPercent / 100.0f)
        }
        g.drawImage(logo, drawX, drawY, drawW, drawH, null)
    } finally {
        g.dispose()
    }
    return canvas
}

fun riplayReadLogo(): BufferedImage {
    if (!riplayLogoPng.isFile) {
        error("[riplay-branding] logo PNG non trovato: ${riplayLogoPng.absolutePath}")
    }
    return ImageIO.read(riplayLogoPng)
        ?: error("[riplay-branding] impossibile leggere il logo: ${riplayLogoPng.absolutePath}")
}

// Estrae il template main.wxs interno a jpackage dal JDK che eseguira' il
// packaging (javaHome dei task Compose): prima dal jmod (zip), poi dal
// filesystem jrt quando javaHome coincide con la JVM di Gradle
fun riplayReadJPackageMainWxsTemplate(javaHome: File): String {
    val jmod = javaHome.resolve("jmods/jdk.jpackage.jmod")
    if (jmod.isFile) {
        ZipFile(jmod).use { zip ->
            val entry = zip.getEntry("classes/jdk/jpackage/internal/resources/main.wxs")
            if (entry != null) {
                return zip.getInputStream(entry).readBytes().toString(Charsets.UTF_8)
            }
        }
    }
    try {
        if (javaHome.absolutePath == File(System.getProperty("java.home")).absolutePath) {
            val jrt = FileSystems.getFileSystem(URI("jrt:/"))
            val templatePath = jrt.getPath("/modules/jdk.jpackage/jdk/jpackage/internal/resources/main.wxs")
            if (Files.exists(templatePath)) {
                return String(Files.readAllBytes(templatePath), Charsets.UTF_8)
            }
        }
    } catch (e: Exception) {
        // filesystem jrt non disponibile: si prosegue verso l'errore esplicito
    }
    error("[riplay-branding] main.wxs di jpackage non trovato nel JDK: ${javaHome.absolutePath}")
}

// Inietta nel template WiX il branding del wizard (WixVariable) e la ricetta
// "lancia l'app a fine installazione" (checkbox + WixShellExec), prima di
// </Product>
fun riplayInjectMsiBranding(template: String, dialogBmp: File, bannerBmp: File): String {
    val closing = "</Product>"
    val idx = template.lastIndexOf(closing)
    require(idx >= 0) { "[riplay-branding] main.wxs inatteso: manca la chiusura </Product>" }

    val additions = StringBuilder()
    additions.appendLine("    <!-- ===== RiPlay STEP 4b: branding wizard + avvio a fine installazione (generato da build.gradle.kts) ===== -->")
    if (dialogBmp.isFile) {
        additions.appendLine("    <WixVariable Id=\"WixUIDialogBmp\" Value=\"${dialogBmp.absolutePath}\"/>")
    }
    if (bannerBmp.isFile) {
        additions.appendLine("    <WixVariable Id=\"WixUIBannerBmp\" Value=\"${bannerBmp.absolutePath}\"/>")
    }
    additions.appendLine("    <Property Id=\"WIXUI_EXITDIALOGOPTIONALCHECKBOXTEXT\" Value=\"$riplayLaunchCheckboxText\"/>")
    additions.appendLine("    <CustomAction Id=\"RiPlaySetLaunchTarget\" Property=\"WixShellExecTarget\" Value=\"&quot;[INSTALLDIR]$riplayPackageName.exe&quot;\"/>")
    additions.appendLine("    <CustomAction Id=\"RiPlayLaunchAppAfterInstall\" BinaryKey=\"WixCA\" DllEntry=\"WixShellExec\" Impersonate=\"yes\"/>")
    additions.appendLine("    <UI>")
    additions.appendLine("      <Publish Dialog=\"ExitDialog\" Control=\"Finish\" Order=\"1\" Event=\"DoAction\" Value=\"RiPlaySetLaunchTarget\">WIXUI_EXITDIALOGOPTIONALCHECKBOX = 1 AND NOT Installed</Publish>")
    additions.appendLine("      <Publish Dialog=\"ExitDialog\" Control=\"Finish\" Order=\"2\" Event=\"DoAction\" Value=\"RiPlayLaunchAppAfterInstall\">WIXUI_EXITDIALOGOPTIONALCHECKBOX = 1 AND NOT Installed</Publish>")
    additions.appendLine("    </UI>")
    additions.appendLine("    <!-- ===== fine branding RiPlay ===== -->")

    return template.substring(0, idx) + additions.toString() + template.substring(idx)
}

// Esegue jpackage in modalita' --app-image con gli stessi parametri che il
// plugin Compose usa per gli installer (nome/descrizione/copyright/versione/
// vendor/licenza) + gli argomenti specifici del formato
fun riplayRunBrandedJPackage(
    task: Task,
    format: String,
    distributableTask: Task,
    resourcesDir: File,
    extraArgs: List<String>,
    needsWix: Boolean
) {
    val dist = distributableTask as AbstractJPackageTask
    val javaHomeDir = File(dist.javaHome.orNull ?: System.getProperty("java.home"))
    val jpackageBin = javaHomeDir.resolve("bin/jpackage${if (riplayWindowsHost) ".exe" else ""}")
    require(jpackageBin.isFile) {
        "[riplay-branding] jpackage non trovato: ${jpackageBin.absolutePath}"
    }

    val distributableDir = dist.destinationDir.get().asFile
    require(distributableDir.isDirectory) {
        "[riplay-branding] app image assente: ${distributableDir.absolutePath}"
    }

    // FIX (jpackage exit 1): --app-image deve puntare alla CARTELLA dell'immagine,
    // NON alla cartella padre: su macOS e' <dest>/RiPlay.app, su Windows/Linux e'
    // <dest>/RiPlay. Stesso schema del plugin Compose (AbstractJPackageTask):
    // appImage.dir(packageName) per non-macOS, appImage.dir("${packageName}.app")
    // per macOS. Con il solo <dest> jpackage esce 1 senza produrre l'installer.
    val appImageDir = if (format == "dmg") {
        distributableDir.resolve("$riplayPackageName.app")
    } else {
        distributableDir.resolve(riplayPackageName)
    }
    require(appImageDir.isDirectory) {
        "[riplay-branding] app image assente: ${appImageDir.absolutePath}"
    }

    // Cartella di output: come i task Compose (build/compose/binaries/<tipo>/<formato>)
    val destDir = distributableDir.parentFile.resolve(format).apply { mkdirs() }

    // Toolset WiX (solo MSI): stesso schema del plugin Compose (wixToolset.kt):
    // variabile d'ambiente WIX_PATH -> WiX scaricato dal plugin (<root>/build/
    // wix311, task downloadWix/unzipWix) -> WiX nel PATH di sistema
    val wixDir: File? = if (needsWix) {
        System.getenv("WIX_PATH")?.let { File(it) }
            ?: rootProject.layout.buildDirectory.dir("wix311").get().asFile.takeIf { it.isDirectory }
    } else {
        null
    }

    val licenseFile = rootProject.file("LICENSE")

    // STEP 4b-fix (Gradle 9): "Unresolved reference 'exec'" su project.exec{}:
    // Gradle 9.0 ha RIMOSSO Project#exec(Action/Closure) e KotlinScript#exec
    // (v. Gradle 9.0 upgrade guide; sostituti ufficiali: ExecOperations
    // iniettato o providers.exec). Il processo parte dentro il doLast di un
    // task anonimo: la via piu' robusta (Gradle 8 e 9, senza deprecazioni ne'
    // refactoring dei task in classi custom) e' ProcessBuilder, con gli stessi
    // identici argomenti e lo stesso PATH WiX di prima.
    val riplayJPackageArgs = buildList {
        add(jpackageBin.absolutePath)
        addAll(
            listOf(
                "--type", format,
                "--app-image", appImageDir.absolutePath,
                "--dest", destDir.absolutePath,
                "--name", riplayPackageName,
                "--description", riplayDescription,
                "--copyright", riplayCopyright,
                "--app-version", riplayPackageVersionForHost,
                "--vendor", riplayVendor,
                "--license-file", licenseFile.absolutePath,
                "--resource-dir", resourcesDir.absolutePath
            )
        )
        addAll(extraArgs)
    }

    val riplayJPackageProc = ProcessBuilder(riplayJPackageArgs)
        .redirectErrorStream(true)
        .apply {
            // Solo MSI: antepone il toolset WiX al PATH, come faceva
            // ExecSpec.environment("PATH", ...) su Project.exec
            if (wixDir != null) {
                val currentPath = environment()["PATH"] ?: ""
                environment()["PATH"] = wixDir.absolutePath + File.pathSeparator + currentPath
            }
        }
        .start()

    // L'output viene consumato in streaming mentre il processo gira (evita il
    // deadlock sul buffer della pipe) e riversato nel log di Gradle
    riplayJPackageProc.inputStream.bufferedReader().forEachLine { line ->
        task.logger.lifecycle("[jpackage:$format] $line")
    }
    val riplayJPackageExit = riplayJPackageProc.waitFor()
    if (riplayJPackageExit != 0) {
        throw GradleException(
            "[riplay-branding] jpackage (--type $format) fallito con exit code $riplayJPackageExit " +
                "(app-image: ${appImageDir.absolutePath})"
        )
    }

    task.logger.lifecycle("[riplay-branding] installer '$format' scritto in: ${destDir.absolutePath}")
}

// Argomenti per-formato dei task branded: specchiano le impostazioni DSL dei
// blocchi windows{}/macOS{} (valori condivisi del STEP 4b)
val riplayMsiExtraArgs = buildList {
    if (riplayWinDirChooser) add("--win-dir-chooser")
    if (riplayWinPerUserInstall) add("--win-per-user-install")
    if (riplayWinShortcut) add("--win-shortcut")
    if (riplayWinMenu) add("--win-menu")
    if (riplayWinUpgradeUuid.isNotEmpty()) {
        add("--win-upgrade-uuid")
        add(riplayWinUpgradeUuid)
    }
}
val riplayDmgExtraArgs = listOf(
    "--mac-package-identifier", riplayMacBundleId
)

// STEP 4b: immagini del wizard MSI (dialog 493x312 + banner 493x58)
val riplayPrepareMsiBranding by tasks.registering {
    group = "compose desktop"
    description = "Genera le immagini del wizard MSI (logo dell'app nel setup di installazione)."
    onlyIf { riplayWindowsHost }
    if (riplayLogoPng.isFile) inputs.file(riplayLogoPng)
    if (riplayCustomMsiDialogBmp.isFile) inputs.file(riplayCustomMsiDialogBmp)
    if (riplayCustomMsiBannerBmp.isFile) inputs.file(riplayCustomMsiBannerBmp)
    outputs.dir(riplayMsiResourcesDir)
    doLast {
        val outDir = riplayMsiResourcesDir.get().asFile
        outDir.mkdirs()
        val dialogBmp = outDir.resolve("dialog.bmp")
        val bannerBmp = outDir.resolve("banner.bmp")

        if (riplayCustomMsiDialogBmp.isFile) {
            riplayCustomMsiDialogBmp.copyTo(dialogBmp, overwrite = true)
        } else if (riplayLogoPng.isFile) {
            // dialog 493x312: logo SOLIDO nella colonna sinistra (box x 30..170,
            // centrato in verticale). Il testo WiX (titolo/descrizione/checkbox
            // di ExitDialog) parte da x=135 dialog-unit = ~180 px e resta su
            // fondo bianco, quindi perfettamente leggibile.
            ImageIO.write(riplayBrandCanvas(riplayReadLogo(), 493, 312, 30, 86, 140, 140, 100), "bmp", dialogBmp)
        } else {
            logger.warn("[riplay-branding] nessun PNG logo in ${riplayDesktopIconsDir.absolutePath}: wizard MSI senza immagine 'dialog'")
        }

        if (riplayCustomMsiBannerBmp.isFile) {
            riplayCustomMsiBannerBmp.copyTo(bannerBmp, overwrite = true)
        } else if (riplayLogoPng.isFile) {
            // banner 493x58: watermark leggero (alpha 18%) a destra (box
            // x 320..465). Titolo (X=15) e descrizione (X=25) WiX sono disegnati
            // sopra il banner per quasi tutta la larghezza: un logo solido
            // finirebbe di nuovo sotto il testo; il watermark su bianco resta
            // leggibile, come l'arte di default di WiX.
            ImageIO.write(riplayBrandCanvas(riplayReadLogo(), 493, 58, 320, 5, 145, 48, 18), "bmp", bannerBmp)
        } else {
            logger.warn("[riplay-branding] nessun PNG logo in ${riplayDesktopIconsDir.absolutePath}: wizard MSI senza immagine 'banner'")
        }

        logger.lifecycle(
            "[riplay-branding] immagini wizard MSI: dialog=${if (dialogBmp.isFile) "ok" else "assente"}, " +
                "banner=${if (bannerBmp.isFile) "ok" else "assente"} (${outDir.absolutePath})"
        )
    }
}

// STEP 4b: sfondo del DMG macOS (520x340, TIFF: nome risorsa atteso da jpackage)
val riplayPrepareDmgBranding by tasks.registering {
    group = "compose desktop"
    description = "Genera lo sfondo brandizzato della finestra di installazione del DMG macOS."
    onlyIf { riplayMacHost }
    if (riplayLogoPng.isFile) inputs.file(riplayLogoPng)
    if (riplayCustomDmgBackgroundPng.isFile) inputs.file(riplayCustomDmgBackgroundPng)
    outputs.dir(riplayDmgResourcesDir)
    doLast {
        val outDir = riplayDmgResourcesDir.get().asFile
        outDir.mkdirs()
        val backgroundTiff = outDir.resolve("background_dmg.tiff")

        val img: BufferedImage? = when {
            riplayCustomDmgBackgroundPng.isFile ->
                ImageIO.read(riplayCustomDmgBackgroundPng)
                    ?: error("[riplay-branding] impossibile leggere: ${riplayCustomDmgBackgroundPng.absolutePath}")
            riplayLogoPng.isFile ->
                riplayBrandCanvas(riplayReadLogo(), 520, 340, 0, 24, 520, 72, 100)
            else -> null
        }
        if (img != null) {
            if (!ImageIO.write(img, "tiff", backgroundTiff)) {
                error("[riplay-branding] writer TIFF non disponibile nel JDK corrente")
            }
            logger.lifecycle("[riplay-branding] sfondo DMG scritto: ${backgroundTiff.absolutePath}")
        } else {
            logger.warn("[riplay-branding] nessun PNG logo in ${riplayDesktopIconsDir.absolutePath}: DMG con sfondo default jpackage")
        }
    }
}

// =============================================================================
// STEP 4c (task jpackage branded): sostituiscono packageMsi/packageDmg (e le
// varianti Release) eseguendo jpackage con --app-image (l'app image e' quella
// di createDistributable/createReleaseDistributable) + la --resource-dir di
// branding (STEP 4b). Per l'MSI il main.wxs brandizzato viene generato qui
// estraendo il template dal JDK che esegue jpackage. I task girano SOLO
// sull'host del formato (onlyIf), come i task Compose. Deb/Rpm restano sui
// task standard (nessun branding applicabile, v. STEP 4b).
// Uso manuale: gradlew packageBrandedMsi | packageReleaseBrandedMsi |
//              packageBrandedDmg | packageReleaseBrandedDmg
// Il CI continua a chiamare packageDistributionForCurrentOS /
// packageReleaseDistributionForCurrentOS: ora producono gli installer
// brandizzati (v. afterEvaluate in fondo al blocco).
// =============================================================================
val riplayBrandedMsi = tasks.register("packageBrandedMsi") {
    group = "compose desktop"
    description = "MSI Windows con logo nel wizard e checkbox di avvio a fine installazione."
    onlyIf { riplayWindowsHost }
    outputs.upToDateWhen { false }
    dependsOn(riplayPrepareMsiBranding)
    doLast {
        val distTask = tasks.getByName("createDistributable") as AbstractJPackageTask
        val resDir = riplayMsiResourcesDir.get().asFile.apply { mkdirs() }
        val javaHomeDir = File(distTask.javaHome.orNull ?: System.getProperty("java.home"))
        val template = riplayReadJPackageMainWxsTemplate(javaHomeDir)
        resDir.resolve("main.wxs").writeText(
            riplayInjectMsiBranding(template, resDir.resolve("dialog.bmp"), resDir.resolve("banner.bmp"))
        )
        logger.lifecycle("[riplay-branding] main.wxs brandizzato scritto in: ${resDir.absolutePath}")
        riplayRunBrandedJPackage(this, "msi", distTask, resDir, riplayMsiExtraArgs, needsWix = true)
    }
}
val riplayBrandedReleaseMsi = tasks.register("packageReleaseBrandedMsi") {
    group = "compose desktop"
    description = "MSI Windows (release) con logo nel wizard e checkbox di avvio a fine installazione."
    onlyIf { riplayWindowsHost }
    outputs.upToDateWhen { false }
    dependsOn(riplayPrepareMsiBranding)
    doLast {
        val distTask = tasks.getByName("createReleaseDistributable") as AbstractJPackageTask
        val resDir = riplayMsiResourcesDir.get().asFile.apply { mkdirs() }
        val javaHomeDir = File(distTask.javaHome.orNull ?: System.getProperty("java.home"))
        val template = riplayReadJPackageMainWxsTemplate(javaHomeDir)
        resDir.resolve("main.wxs").writeText(
            riplayInjectMsiBranding(template, resDir.resolve("dialog.bmp"), resDir.resolve("banner.bmp"))
        )
        logger.lifecycle("[riplay-branding] main.wxs brandizzato scritto in: ${resDir.absolutePath}")
        riplayRunBrandedJPackage(this, "msi", distTask, resDir, riplayMsiExtraArgs, needsWix = true)
    }
}
val riplayBrandedDmg = tasks.register("packageBrandedDmg") {
    group = "compose desktop"
    description = "DMG macOS con sfondo brandizzato."
    onlyIf { riplayMacHost }
    outputs.upToDateWhen { false }
    dependsOn(riplayPrepareDmgBranding)
    doLast {
        val distTask = tasks.getByName("createDistributable") as AbstractJPackageTask
        riplayRunBrandedJPackage(this, "dmg", distTask, riplayDmgResourcesDir.get().asFile, riplayDmgExtraArgs, needsWix = false)
    }
}
val riplayBrandedReleaseDmg = tasks.register("packageReleaseBrandedDmg") {
    group = "compose desktop"
    description = "DMG macOS (release) con sfondo brandizzato."
    onlyIf { riplayMacHost }
    outputs.upToDateWhen { false }
    dependsOn(riplayPrepareDmgBranding)
    doLast {
        val distTask = tasks.getByName("createReleaseDistributable") as AbstractJPackageTask
        riplayRunBrandedJPackage(this, "dmg", distTask, riplayDmgResourcesDir.get().asFile, riplayDmgExtraArgs, needsWix = false)
    }
}

afterEvaluate {
    if (!riplayInstallerBrandingEnabled) {
        logger.lifecycle("[riplay-branding] disattivato (-PriplayInstallerBranding=false): task Msi/Dmg standard senza modifiche")
        return@afterEvaluate
    }

    // I task distributable di Compose devono esistere a questo punto: il
    // plugin li registra nel proprio afterEvaluate (eseguito prima di questo)
    listOf("createDistributable", "createReleaseDistributable").forEach { name ->
        require(tasks.findByName(name) != null) {
            "[riplay-branding] task '$name' non trovato: versione del plugin Compose inattesa"
        }
    }

    // 1) disattiva i package Msi/Dmg standard (sostituiti dai branded)
    listOf("packageMsi", "packageReleaseMsi", "packageDmg", "packageReleaseDmg").forEach { name ->
        tasks.matching { it.name == name }.configureEach {
            enabled = false
        }
    }

    // 2) collega i branded ai rispettivi task distributable + preparazione
    //    risorse + (Windows) WiX scaricato dal plugin, come fa wixToolset.kt
    fun riplayWireBranded(
        branded: TaskProvider<Task>,
        distributableTaskName: String,
        prepTask: TaskProvider<*>,
        needsWix: Boolean
    ) {
        branded.configure {
            dependsOn(tasks.getByName(distributableTaskName))
            dependsOn(prepTask)
            if (needsWix && riplayWindowsHost) {
                rootProject.tasks.findByName("unzipWix")?.let { dependsOn(it) }
            }
        }
    }
    riplayWireBranded(riplayBrandedMsi, "createDistributable", riplayPrepareMsiBranding, needsWix = true)
    riplayWireBranded(riplayBrandedReleaseMsi, "createReleaseDistributable", riplayPrepareMsiBranding, needsWix = true)
    riplayWireBranded(riplayBrandedDmg, "createDistributable", riplayPrepareDmgBranding, needsWix = false)
    riplayWireBranded(riplayBrandedReleaseDmg, "createReleaseDistributable", riplayPrepareDmgBranding, needsWix = false)

    // 3) gli aggregatori (usati dal workflow CI) producono ora gli installer
    //    brandizzati; Deb/Rpm restano sui task standard
    tasks.matching {
        it.name == "packageDistributionForCurrentOS" || it.name == "packageReleaseDistributionForCurrentOS"
    }.configureEach {
        dependsOn(riplayBrandedMsi, riplayBrandedReleaseMsi, riplayBrandedDmg, riplayBrandedReleaseDmg)
    }

    // 4) ridondanza di sicurezza: se qualcuno chiama direttamente
    //    packageMsi/packageDmg (disattivati), i branded partono comunque
    //    come finalizzatori
    tasks.matching { it.name == "packageMsi" || it.name == "packageReleaseMsi" }.configureEach {
        finalizedBy(riplayBrandedMsi, riplayBrandedReleaseMsi)
    }
    tasks.matching { it.name == "packageDmg" || it.name == "packageReleaseDmg" }.configureEach {
        finalizedBy(riplayBrandedDmg, riplayBrandedReleaseDmg)
    }

    logger.lifecycle("[riplay-branding] attivo: packageMsi/packageDmg (+Release) sostituiti dai task branded")
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
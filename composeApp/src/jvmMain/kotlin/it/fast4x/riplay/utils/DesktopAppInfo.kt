package it.fast4x.riplay.utils

/**
 * STEP 4b: metadati dell'app desktop, usati dalla tab Info (About) delle
 * impostazioni e dallo splashscreen di avvio.
 *
 * NB: jpackage non espone a runtime la versione del pacchetto, quindi vive
 * qui come costante: tenere VERSION allineata con riplayDesktopVersion in
 * composeApp/build.gradle.kts (attualmente "0.8.3").
 */
object DesktopAppInfo {
    const val VERSION = "0.8.3"
    const val VENDOR = "RiPlay.DesktopApp"

    /** Testo di presentazione condiviso da splashscreen e tab Info. */
    const val TAGLINE = "by fast4x"
}

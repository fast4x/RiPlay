package it.fast4x.riplay.musicvault

import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.utils.appContext
import timber.log.Timber

fun checkAndStartMusicVault(){
    val context = appContext()

    val appSettingsManager = (context as MainApplication).appSettingsManager
    val appSettings = appSettingsManager.activeSettings.value

//    if (context.preferences.getBoolean(MUSIC_VAULT_ENABLED.key, false)
//        && context.preferences.getBoolean(MUSIC_VAULT_DISCLAIMER_ACCEPTED.key, false)) {
    if (appSettings.musicVaultEnabled && appSettings.musicVaultDisclaimerAccepted) {
        val result = testAndStartChaquopy()
        Timber.d("Chaquopy $result")
    }
}

private fun testAndStartChaquopy(): Triple<String, String, Boolean> {
    return engine.testAndStartChaquopy(appContext())
    /*
    if (!Python.isStarted()) {
        Python.start(AndroidPlatform(appContext()))
    }

    val py = Python.getInstance()

    // Test 1: Python funziona?
    val sys = py.getModule("sys")
    val pyVersion = sys["version"].toString()
    Timber.d("Chaquopy Python version: $pyVersion")

    // Test 2: yt-dlp è installato?
    val ytdlp = py.getModule("yt_dlp")
    val ytdlpVersion = ytdlp["version"]?.get("__version__").toString()
    Timber.d("Chaquopy yt-dlp version: $ytdlpVersion")

    val ytdlpIsReady = (pyVersion.isNotEmpty() && ytdlpVersion.isNotEmpty())

    return Triple(pyVersion, ytdlpVersion, ytdlpIsReady)

     */
}

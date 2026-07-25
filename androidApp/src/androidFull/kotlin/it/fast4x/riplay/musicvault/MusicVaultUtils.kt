package it.fast4x.riplay.musicvault

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.utils.appContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


fun initializeMusicVault(coroutineScope: CoroutineScope, context: Context) {
    coroutineScope.launch(Dispatchers.IO) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
    }
}

fun checkAndStartMusicVault(){
    val context = appContext()

    val appSettingsManager = (context as MainApplication).appSettingsManager
    val appSettings = appSettingsManager.activeSettings.value

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

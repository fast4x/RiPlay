package it.fast4x.riplay.utils

import androidx.compose.runtime.Composable
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.ui.components.themed.NewVersionDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import okhttp3.Request
import timber.log.Timber

fun downloadNewVersionInfo() {

    CoroutineScope(Dispatchers.IO).launch {
        val url =
            "https://raw.githubusercontent.com/fast4x/RiPlay/main/updatedVersion/updatedVersionCode.ver"
        val client = CustomHttpClient.okHttpClient

        try {
            val response =
                client.newCall(Request.Builder().url(url).build()).executeAsync()

            val content = response.body.string().split("-")
            Timber.d("CheckUpdate UpdatedVersionCode Check success $content")
            withContext(Dispatchers.Main) {
                GlobalSharedData.versionCode.value = content.firstOrNull()?.toInt() ?: 0
                GlobalSharedData.versionName.value = content.getOrNull(1) ?: ""
                GlobalSharedData.productName.value = content.getOrNull(2) ?: ""
            }

        } catch (e: IOException) {
            Timber.d("UpdatedVersionCode Check failure ${e.message}")
        } catch (e: Exception) {
            Timber.d("UpdatedVersionCode Generic error ${e.message}")
        }
    }

}


@Composable
fun CheckForNewVersion(
    onDismiss: () -> Unit,
    updateAvailable: (Boolean) -> Unit
) {
    val (updatedVersionName, updatedProductName, updatedVersionCode) = getAvailableUpdateInfo()

    Timber.d("CheckUpdate CheckAvailableNewVersion: $updatedVersionName $updatedProductName $updatedVersionCode")

    if (updatedVersionCode > BuildConfig.VERSION_CODE) {
        NewVersionDialog(
            updatedVersionName = updatedVersionName,
            updatedVersionCode = updatedVersionCode,
            updatedProductName = updatedProductName,
            onDismiss = onDismiss
        )
        updateAvailable(true)
    } else {
        updateAvailable(false)
        onDismiss()
    }
}

fun getAvailableUpdateInfo(): Triple<String, String, Int> {
    return Triple(GlobalSharedData.productName.value, GlobalSharedData.versionName.value,
        GlobalSharedData.versionCode.value)
}

fun getUpdateDownloadUrl(): String {
    val updatedVersionName = if (GlobalSharedData.versionName.value == "")
        GlobalSharedData.versionName.value else BuildConfig.VERSION_NAME

    return "https://github.com/fast4x/RiPlay/releases/download/v$updatedVersionName/RiPlay-${BuildConfig.BUILD_VARIANT}-release-$updatedVersionName.apk"
}

package it.fast4x.riplay.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.ui.components.themed.NewVersionDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import timber.log.Timber

suspend fun downloadNewVersionInfo(): Result<Triple<Int, String, String>> =
    withContext(Dispatchers.IO) {
        runCatching {
            val url =
                "https://raw.githubusercontent.com/fast4x/RiPlay/main/updatedVersion/updatedVersionCode.ver"
            val client = CustomHttpClient.okHttpClient
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val content = response.body.string().split("-")

            Triple(
                content.firstOrNull()?.toInt() ?: 0,
                content.getOrNull(1) ?: "",
                content.getOrNull(2) ?: ""
            )

        }.onFailure { e ->
            Timber.d("UpdatedVersionCode Check failure ${e.message}")
            Triple(0, "", "")
        }
    }


@Composable
fun CheckForNewVersion(
    onDismiss: () -> Unit,
    onClose: () -> Unit,
    onNoUpdateAvailable: () -> Unit
) {
    val (updatedVersionCode, updatedVersionName, updatedProductName) = Triple(0,"","")
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        downloadNewVersionInfo()
            .onSuccess { (newVersionCode, newVersionName, newProductName) ->
                if (newVersionCode > BuildConfig.VERSION_CODE) showDialog = true else onNoUpdateAvailable()
                Timber.d("CheckForNewVersion Success $newVersionCode $newVersionName $newProductName")
            }
            .onFailure {
                onNoUpdateAvailable()
                Timber.d("CheckForNewVersion Failure ${it.message}")
            }
    }

    if (showDialog) {
        NewVersionDialog(
            updatedVersionName = updatedVersionName,
            updatedVersionCode = updatedVersionCode,
            updatedProductName = updatedProductName,
            onClose = onClose,
            onDismiss = {},
        )
    }
}


fun getUpdateDownloadUrl(updatedVersionName: String): String {
    return "https://github.com/fast4x/RiPlay/releases/download/v$updatedVersionName/RiPlay-${BuildConfig.BUILD_VARIANT}-release-$updatedVersionName.apk"
}

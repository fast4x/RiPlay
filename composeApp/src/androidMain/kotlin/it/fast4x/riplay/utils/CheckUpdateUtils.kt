package it.fast4x.riplay.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import okhttp3.Request
import timber.log.Timber
import java.io.File

suspend fun checkAndDownloadNewVersionCode() {

    CoroutineScope(Dispatchers.Default).launch {
        val url =
            "https://raw.githubusercontent.com/fast4x/RiPlay/main/updatedVersion/updatedVersionCode.ver"
        val client = okHttpClient() //OkHttpClient()

        try {
            val response =
                client.newCall(Request.Builder().url(url).build()).executeAsync()

            val content = response.body.string()

            withContext(Dispatchers.IO) {
                File(appContext().filesDir, "UpdatedVersionCode.ver").writeText(content)
            }

        } catch (e: IOException) {
            Timber.d("UpdatedVersionCode Check failure ${e.message}")
        } catch (e: Exception) {
            Timber.d("UpdatedVersionCode Generic error ${e.message}")
        }
    }

}
package it.fast4x.riplay.extensions.spotifreeupdater

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.ui.styling.Encore
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks the Spotifree GitHub repository (built automatically from upstream RiPlay + our changes)
 * for a newer release and offers to download it.
 * Release tags are "sf-<build number>"; each APK knows its own number in BuildConfig.SPOTIFREE_BUILD.
 */
object SpotifreeUpdater {
    data class Release(val build: Int, val apkUrl: String, val notes: String)

    @Volatile private var checkedThisSession = false

    suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://api.github.com/repos/${BuildConfig.SPOTIFREE_REPO}/releases/latest")
                .openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != 200) return@runCatching null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = Json.parseToJsonElement(body).jsonObject
            val tag = json["tag_name"]?.jsonPrimitive?.content ?: return@runCatching null
            val build = tag.removePrefix("sf-").toIntOrNull() ?: return@runCatching null
            val apk = json["assets"]?.jsonArray
                ?.map { it.jsonObject }
                ?.firstOrNull { it["name"]?.jsonPrimitive?.content?.endsWith(".apk") == true }
                ?.get("browser_download_url")?.jsonPrimitive?.content
                ?: return@runCatching null
            val notes = json["body"]?.jsonPrimitive?.content.orEmpty()
            Release(build, apk, notes)
        }.onFailure { Timber.w(it, "SpotifreeUpdater check failed") }.getOrNull()
    }

    /** Shows an "Update available" dialog once per app session when a newer build exists. */
    @Composable
    fun Check() {
        // Builds made by hand in Android Studio (build 0) never nag.
        if (BuildConfig.SPOTIFREE_BUILD == 0) return
        val context = LocalContext.current
        var release by remember { mutableStateOf<Release?>(null) }

        LaunchedEffect(Unit) {
            if (checkedThisSession) return@LaunchedEffect
            checkedThisSession = true
            latest()?.takeIf { it.build > BuildConfig.SPOTIFREE_BUILD }?.let { release = it }
        }

        val r = release ?: return
        Dialog(onDismissRequest = { release = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Encore.SurfaceElevated)
                    .padding(24.dp)
            ) {
                BasicText("Update available", style = typography().l.bold.copy(color = Color.White))
                Spacer(Modifier.height(8.dp))
                BasicText(
                    "A new version of Spotifree is ready (build ${r.build}). It includes the latest changes from RiPlay.",
                    style = typography().xs.copy(color = Encore.TextSecondary)
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BasicText(
                        "Not now",
                        style = typography().s.semiBold.copy(color = Color.White),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { release = null }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                    BasicText(
                        "Update",
                        style = typography().s.bold.copy(color = Encore.OnGreen),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Encore.Green)
                            .clickable {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, r.apkUrl.toUri())
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                                release = null
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

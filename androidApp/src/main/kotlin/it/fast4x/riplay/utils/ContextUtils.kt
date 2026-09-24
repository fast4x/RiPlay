package it.fast4x.riplay.utils

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.PowerManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.offline.DownloadService.sendAddDownload
import androidx.media3.exoplayer.offline.DownloadService.sendRemoveDownload
import android.net.Uri
import android.provider.OpenableColumns
import timber.log.Timber
import java.io.File

inline fun <reified T> Context.intent(): Intent =
    Intent(this, T::class.java)

inline fun <reified T : BroadcastReceiver> Context.broadCastPendingIntent(
    requestCode: Int = 0,
    flags: Int = if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0,
): PendingIntent =
    PendingIntent.getBroadcast(this, requestCode, intent<T>(), flags)

inline fun <reified T : Activity> Context.activityPendingIntent(
    requestCode: Int = 0,
    flags: Int = 0,
    block: Intent.() -> Unit = {},
): PendingIntent =
    PendingIntent.getActivity(
        this,
        requestCode,
        intent<T>().apply(block),
        (if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0) or flags
    )

fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(packageName)
}

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
fun Context.toastLong(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

fun Context.hasPermission(permission: String) = ContextCompat.checkSelfPermission(
    applicationContext,
    permission
) == PackageManager.PERMISSION_GRANTED

/*
fun launchYouTubeMusic(
    context: Context,
    endpoint: String,
    tryWithoutBrowser: Boolean = true
): Boolean {
    return try {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://music.youtube.com/${endpoint.dropWhile { it == '/' }}")
        ).apply {
            if (tryWithoutBrowser && isAtLeastAndroid11) {
                flags = Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER
            }
        }
        intent.`package` =
            context.applicationContext.packageManager.queryIntentActivities(intent, 0)
                .firstOrNull {
                    it?.activityInfo?.packageName != null &&
                            BuildConfig.APPLICATION_ID !in it.activityInfo.packageName
                }?.activityInfo?.packageName
                ?: return false
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        if (tryWithoutBrowser) launchYouTubeMusic(
            context = context,
            endpoint = endpoint,
            tryWithoutBrowser = false
        ) else false
    }
}
 */

@OptIn(UnstableApi::class)
inline fun <reified T : DownloadService> Context.download(request: DownloadRequest) = runCatching {
    sendAddDownload(
        /* context         = */ this,
        /* clazz           = */ T::class.java,
        /* downloadRequest = */ request,
        /* foreground      = */ true
    )
}.recoverCatching {
    sendAddDownload(
        /* context         = */ this,
        /* clazz           = */ T::class.java,
        /* downloadRequest = */ request,
        /* foreground      = */ false
    )
}

@OptIn(UnstableApi::class)
inline fun <reified T : DownloadService> Context.removeDownload(mediaId: String) = runCatching {
    sendRemoveDownload(
        /* context         = */ this,
        /* clazz           = */ T::class.java,
        /* id              = */ mediaId,
        /* foreground      = */ false
    )
}

fun Context.isConnectionMetered(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return manager.isActiveNetworkMetered
}

fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    error("Should be called in the context of an Activity")
}

fun Context.getFileNameFromUri(uri: Uri): String? {
    // Caso 1: Uri di tipo "file://" (es. /storage/emulated/0/Music/canzone.mp3)
    if (uri.scheme == "file") {
        return uri.lastPathSegment ?: File(uri.path ?: return null).name
    }

    // Caso 2: Uri di tipo "content://" (es. content://media/external/audio/media/123 o SAF)
    if (uri.scheme == "content") {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // La colonna DISPLAY_NAME contiene il nome del file con estensione
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        return cursor.getString(nameIndex)
                    }
                }
                null
            }
        } catch (e: Exception) {
            // Se la query fallisce (es. permessi mancanti o Uri non più valida)
            //Timber.e("${ e.printStackTrace() }")
            null
        }

        // Fallback se la query non funziona: tentiamo di prendere l'ultimo segmento del path
        // Es. content://com.android.externalstorage.documents/document/primary%3AMusic%2Fsong.mp3
        return uri.lastPathSegment?.substringAfterLast("/")
    }

    // Caso 3: Altri schemi (es. http/https)
    return uri.lastPathSegment ?: uri.path?.substringAfterLast("/")
}
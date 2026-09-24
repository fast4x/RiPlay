package it.fast4x.riplay.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.media.MediaScannerConnection
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.themed.SecondaryTextButton
import it.fast4x.riplay.ui.components.themed.SmartMessage
import androidx.core.net.toUri
import it.fast4x.riplay.enums.PopupType

suspend fun setRingtoneSmart(
    context: Context,
    sourceFileUri: Uri,
    fileName: String = "Riplay_Ringtone"
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            if (!Settings.System.canWrite(context)) {
                return@withContext false
            }

            val resolver = context.contentResolver
            val displayName = "$fileName.mp3"
            var ringtoneUri: Uri? = null
            var isExistingFile = false

            val projection = arrayOf(MediaStore.Audio.Media._ID)
            val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(displayName)

            var realSelection = selection
            var realArgs = selectionArgs

            if (isAtLeastAndroid10) {
                realSelection = "$selection AND ${MediaStore.Audio.Media.RELATIVE_PATH} = ?"
                realArgs = arrayOf(displayName, Environment.DIRECTORY_RINGTONES)
            }

            val cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                realSelection,
                realArgs,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    ringtoneUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    isExistingFile = true
                    Timber.d("Ringtone found existing file: $ringtoneUri")
                }
            }

            if (ringtoneUri == null) {
                Timber.d("Ringtone file created...")
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.TITLE, fileName)
                    put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3")
                    put(MediaStore.Audio.Media.IS_RINGTONE, true)
                    put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                    put(MediaStore.Audio.Media.IS_ALARM, false)
                    put(MediaStore.Audio.Media.IS_MUSIC, false)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
                        put(MediaStore.Audio.Media.IS_PENDING, 1) // Metti in pending mentre scrivi
                    }
                }
                ringtoneUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                isExistingFile = false
            }

            if (ringtoneUri != null) {
                var copySuccess = false
                resolver.openInputStream(sourceFileUri)?.use { input ->
                    resolver.openOutputStream(ringtoneUri!!, "wt")?.use { output ->
                        input.copyTo(output)
                        copySuccess = true
                    }
                }

                if (!copySuccess) {
                    Timber.e("Ringtone Critical error when copying file")
                    return@withContext false
                }

                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_RINGTONE, true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Audio.Media.IS_PENDING, 0)
                    }
                }
                resolver.update(ringtoneUri, values, null, null)

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(ringtoneUri.toString()),
                    null
                ) { path, uri ->
                    Timber.d("Ringtone Scan completed for: $path -> $uri")
                }

                Settings.System.putString(
                    resolver,
                    Settings.System.RINGTONE,
                    ringtoneUri.toString()
                )

                return@withContext true
            }

            return@withContext false

        } catch (e: Exception) {
            Timber.e(e, "Ringtone Exception: ${e.message}")
            return@withContext false
        }
    }
}

@Composable
fun SetupWriteSettingsPermission(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current

    var canWriteSettings by remember { mutableStateOf(Settings.System.canWrite(context)) }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        canWriteSettings = Settings.System.canWrite(context)

        if (canWriteSettings) {
            SmartMessage(context.resources.getString(R.string.permission_granted),
                context = context
            )
            onPermissionGranted()
        } else {
            SmartMessage(context.resources.getString(R.string.permission_denied),
                type = PopupType.Warning,
                context = context
            )
        }
    }

    LaunchedEffect(canWriteSettings) {
        if (canWriteSettings) {
            onPermissionGranted()
        }
    }

    if (!canWriteSettings) {
        SecondaryTextButton(
            stringResource(R.string.permission_required),
            onClick = {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = ("package:" + context.packageName).toUri()
                settingsLauncher.launch(intent)
            }
        )

    }
}
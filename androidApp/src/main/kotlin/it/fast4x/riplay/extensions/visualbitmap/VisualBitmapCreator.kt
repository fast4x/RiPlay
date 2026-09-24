package it.fast4x.riplay.extensions.visualbitmap

import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.utils.colorPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun VisualBitmapCreator(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    val isGenerating = remember { mutableStateOf(false) }
    val isGenerated = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()


    Column(modifier = modifier.background(Color.Transparent)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            content()

            val bg = colorPalette().background0
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .applyIf(isGenerated.value) {
                        background(bg)
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isGenerated.value) Arrangement.SpaceAround else Arrangement.Start
                ) {
                    if (!isGenerating.value && bitmap.value == null) {
                        IconButton(
                            onClick = {
                                isGenerating.value = true
                                coroutineScope.launch {
                                    delay(1000)
                                    val generatedBitmap = generateBitmapFromViewSafely(view)

                                    bitmap.value = generatedBitmap
                                    isGenerating.value = false
                                    bitmap.value?.width?.let { isGenerated.value = it > 1 }
                                    Timber.d("VisualBitmapCreator: Generated bitmap: ${isGenerated.value}")
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.camera),
                                null,
                                modifier
                                    .size(26.dp)
                                    .padding(horizontal = 4.dp),
                                tint = colorPalette().text
                            )
                        }

                    }

                    if (isGenerated.value) {
                        IconButton(
                            onClick = {
                                bitmap.value = null
                                isGenerated.value = false
                                isGenerating.value = false
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                null,
                                modifier
                                    .size(32.dp)
                                    .padding(horizontal = 4.dp),
                                tint = colorPalette().text
                            )
                        }

                        IconButton(
                            onClick = {
                                bitmap.value?.let { bmp ->
                                    shareImage(context, bmp)
                                } ?: run {
                                    SmartMessage(
                                        "No image to share",
                                        PopupType.Error,
                                        context = context
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.share_social),
                                null,
                                modifier
                                    .size(32.dp)
                                    .padding(horizontal = 4.dp),
                                tint = colorPalette().text
                            )
                        }
                    }
                }

                bitmap.value?.let { bmp ->
                    if (bmp.width == 1) return@let

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                            //.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Created image",
                            modifier = Modifier.size(300.dp)
                        )
//                        IconButton(
//                            onClick = {
//                                bitmap.value?.let { bmp ->
//                                    shareImage(context, bmp)
//                                } ?: run {
//                                    SmartMessage(
//                                        "No image to share",
//                                        PopupType.Error,
//                                        context = context
//                                    )
//                                }
//                            },
//                        ) {
//                            Icon(
//                                painter = painterResource(R.drawable.chevron_forward),
//                                null,
//                                modifier.size(32.dp)
//                                    .padding(horizontal = 4.dp),
//                                tint = colorPalette().text
//                            )
//                        }

                    }
                }
            }
        }
    }
}

suspend fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}

fun shareImage(context: android.content.Context, bitmap: Bitmap) {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "image_$timeStamp.png"
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)

    try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share image using..."))

    } catch (e: Exception) {
        e.printStackTrace()
    }
}
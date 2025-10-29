package it.fast4x.riplay.extensions.ondevice

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.isAtLeastAndroid13

@Composable
fun OnDeviceLoader() {
    val context = LocalContext.current
    val onDeviceViewModel: OnDeviceViewModel = viewModel()
    val audioFiles by onDeviceViewModel.audioFiles.collectAsState()
    //Timber.d("Permission granted, on device songs loaded successfully ${audioFiles.size}")

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onDeviceViewModel.loadAudioFiles()
        } else {
            SmartMessage("Permession not granted, please grant it", context = context)
        }
    }

    val permission = if (isAtLeastAndroid13) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    SideEffect {
        permissionLauncher.launch(permission)
    }


}


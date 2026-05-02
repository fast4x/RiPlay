package it.fast4x.riplay.ui.screens.onboarding

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.isAtLeastAndroid6
import it.fast4x.riplay.utils.isIgnoringBatteryOptimizations


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current


    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            refreshTrigger++
        }
    }

    val batteryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            refreshTrigger++
        }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val msgNoBatteryOptim =
        stringResource(R.string.not_find_battery_optimization_settings)


    val items = remember(refreshTrigger) {
        buildList {
            add(OnboardingSection(title = context.resources.getString(R.string.onboard_title_permissions)))


            val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkPermission(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                true
            }

            add(
                OnboardingItem(
                    id = "notifications",
                    title = context.resources.getString(R.string.onboard_perm_notifications),
                    description = context.resources.getString(R.string.onboard_notifications_get_media_player_in_the_notifications),
                    icon = R.drawable.notifications,
                    status = if (notificationGranted) PermissionStatus.GRANTED else PermissionStatus.NOT_REQUESTED,
                    onRequest = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            )


            val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkPermission(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            add(
                OnboardingItem(
                    id = "storage",
                    title = context.resources.getString(R.string.onboard_perm_accessing_files_on_disk),
                    description = context.resources.getString(R.string.onboard_storage_allow_the_app_to_read_the_music_library),
                    icon = R.drawable.folder,
                    status = if (storageGranted) PermissionStatus.GRANTED else PermissionStatus.NOT_REQUESTED,
                    onRequest = {
                        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_AUDIO
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        permissionLauncher.launch(perm)
                    }
                )
            )

            val btGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkPermission(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                true
            }

            add(
                OnboardingItem(
                    id = "bluetooth",
                    title = context.resources.getString(R.string.onboard_perm_bluetooth),
                    description = context.resources.getString(R.string.onboard_bluetooth_detect_headphones_to_pause_or_resume_music),
                    icon = R.drawable.bluetooth,
                    status = if (btGranted) PermissionStatus.GRANTED else PermissionStatus.NOT_REQUESTED,
                    onRequest = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    }
                )
            )

            val micGranted = checkPermission(Manifest.permission.RECORD_AUDIO)

            add(
                OnboardingItem(
                    id = "mic",
                    title = context.resources.getString(R.string.onboard_perm_microphone),
                    description = context.resources.getString(R.string.onboard_mic_allows_you_to_use_the_microphone_for_voice_commands_the_visualizer_and_other_features),
                    icon = R.drawable.mic,
                    status = if (micGranted) PermissionStatus.GRANTED else PermissionStatus.NOT_REQUESTED,
                    onRequest = {
                        val perm = Manifest.permission.RECORD_AUDIO
                        permissionLauncher.launch(perm)
                    }
                )
            )

            val isIgnoringBattery = context.isIgnoringBatteryOptimizations()
            add(
                OnboardingItem(
                    id = "battery",
                    title = context.resources.getString(R.string.onboard_perm_battery_optimization),
                    description = context.resources.getString(R.string.onboard_battery_prevent_the_system_from_stopping_background_music),
                    icon = R.drawable.battery_charging,
                    status = if (isIgnoringBattery) PermissionStatus.GRANTED else PermissionStatus.NOT_REQUESTED,
                    onRequest = {
                        if (!isAtLeastAndroid6) return@OnboardingItem

                        try {
                            batteryLauncher.launch(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                            )
                        } catch (e: ActivityNotFoundException) {
                            try {
                                batteryLauncher.launch(
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                )
                            } catch (e: ActivityNotFoundException) {
                                SmartMessage(
                                    "$msgNoBatteryOptim RiPlay",
                                    type = PopupType.Info,
                                    context = context
                                )
                            }
                        }
                    }
                )
            )
        }
    }

    Scaffold { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboard_hello_grant_the_useful_permissions_to_configure_your_perfect_music_experience),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items) { item ->
                    when (item) {
                        is OnboardingItem -> PermissionCard(item = item)
                        is OnboardingSection -> PermissionSection(item = item)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onComplete() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboard_start_riplay))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSection(item: OnboardingSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionCard(item: OnboardingItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))


            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))


            ActionButton(status = item.status, onClick = item.onRequest)
        }
    }
}

@Composable
fun ActionButton(status: PermissionStatus, onClick: () -> Unit) {
    when (status) {
        PermissionStatus.GRANTED -> {
            Icon(
                painter = painterResource(id = R.drawable.checkmark),
                contentDescription = stringResource(R.string.onboard_permission_granted),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        PermissionStatus.NOT_REQUESTED, PermissionStatus.DENIED -> {
            OutlinedButton(onClick = onClick) {
                Text(stringResource(R.string.onboard_permission_grant))
            }
        }
        PermissionStatus.PERMANENTLY_DENIED -> {
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.onboard_permission_settings))
            }
        }
    }
}
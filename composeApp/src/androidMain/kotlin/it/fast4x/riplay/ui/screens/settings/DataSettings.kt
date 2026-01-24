package it.fast4x.riplay.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import it.fast4x.riplay.LocalBackupManager
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.CacheType
import it.fast4x.riplay.enums.CoilDiskCacheMaxSize
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.ui.components.themed.CacheSpaceIndicator
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.InputNumericDialog
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.utils.RestartPlayerService
import it.fast4x.riplay.extensions.preferences.coilCustomDiskCacheKey
import it.fast4x.riplay.extensions.preferences.coilDiskCacheMaxSizeKey
import it.fast4x.riplay.extensions.preferences.pauseSearchHistoryKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import kotlinx.coroutines.flow.distinctUntilChanged
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.extensions.databasebackup.BackupUiState
import it.fast4x.riplay.extensions.databasebackup.BackupViewModel
import it.fast4x.riplay.extensions.databasebackup.DatabaseBackupManager
import it.fast4x.riplay.ui.components.themed.SmartMessage
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.compareTo

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalCoilApi::class)
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun DataSettings() {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current

    /*
    // single instance
    val backupViewModel: BackupViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
                    return BackupViewModel(DatabaseBackupManager(context, Database), context) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    )
    */

    val backupViewModel = LocalBackupManager.current
    val backupUiState by backupViewModel.uiState.collectAsState()

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        backupViewModel.performBackup(uri)
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        backupViewModel.performRestore(uri)
    }

    LaunchedEffect(backupUiState) {
        when (val state = backupUiState) {
            is BackupUiState.Success -> {
                SmartMessage(state.message, context = context, durationLong = true, type = PopupType.Success)
                backupViewModel.clearState()
            }
            is BackupUiState.Error -> {
                SmartMessage(state.message, context = context, durationLong = true, type = PopupType.Error)
                backupViewModel.clearState()
            }
            else -> {}
        }
    }

    var coilDiskCacheMaxSize by rememberPreference(
        coilDiskCacheMaxSizeKey,
        CoilDiskCacheMaxSize.`128MB`
    )


    var showCoilCustomDiskCacheDialog by remember { mutableStateOf(false) }
    var coilCustomDiskCache by rememberPreference(
        coilCustomDiskCacheKey,32
    )
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    if (isExporting) {
        ConfirmationDialog(
            text = stringResource(R.string.export_the_database),
            onDismiss = { isExporting = false },
            onConfirm = {
                @SuppressLint("SimpleDateFormat")
                val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                backupLauncher.launch("riplay_${dateFormat.format(Date())}.db")
            }
        )
    }
    if (isImporting) {
        ConfirmationDialog(
            text = stringResource(R.string.import_the_database),
            onDismiss = { isImporting = false },
            onConfirm = {
                restoreLauncher.launch(arrayOf("application/octet-stream"))
            }
        )
    }

    var pauseSearchHistory by rememberPreference(pauseSearchHistoryKey, false)

    val queriesCount by remember {
        Database.queriesCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    var cleanCacheOfflineSongs by remember {
        mutableStateOf(false)
    }

    var cleanCacheImages by remember {
        mutableStateOf(false)
    }

//    if (cleanCacheOfflineSongs) {
//        ConfirmationDialog(
//            text = stringResource(R.string.do_you_really_want_to_delete_cache),
//            onDismiss = {
//                cleanCacheOfflineSongs = false
//            },
//            onConfirm = {
//                binder?.cache?.keys?.forEach { song ->
//                    binder.cache.removeResource(song)
//                }
//            }
//        )
//    }

    if (cleanCacheImages) {
        ConfirmationDialog(
            text = stringResource(R.string.do_you_really_want_to_delete_cache),
            onDismiss = {
                cleanCacheImages = false
            },
            onConfirm = {
                Coil.imageLoader(context).diskCache?.clear()
            }
        )
    }

//    val eventsCount by remember {
//        Database.eventsCount().distinctUntilChanged()
//    }.collectAsState(initial = 0)
//    var clearEvents by remember { mutableStateOf(false) }
//    if (clearEvents) {
//        ConfirmationDialog(
//            text = stringResource(R.string.do_you_really_want_to_delete_all_playback_events),
//            onDismiss = { clearEvents = false },
//            onConfirm = { Database.asyncTransaction( Database::clearEvents ) }
//        )
//    }

    var restartService by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(colorPalette().background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
            .verticalScroll(rememberScrollState())

    ) {
        HeaderWithIcon(
            title = stringResource(R.string.tab_data),
            iconId = R.drawable.server,
            enabled = false,
            showIcon = true,
            modifier = Modifier,
            onClick = {}
        )

        SettingsDescription(text = stringResource(R.string.cache_cleared))

        Coil.imageLoader(context).diskCache?.let { diskCache ->
            val diskCacheSize = remember(diskCache.size, cleanCacheImages) {
                diskCache.size
            }

            SettingsGroupSpacer()
            SettingsEntryGroupText(title = stringResource(R.string.cache))

            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.image_cache_max_size),
                titleSecondary = when (coilDiskCacheMaxSize) {
                    CoilDiskCacheMaxSize.Custom -> Formatter.formatShortFileSize(context, diskCacheSize) +
                            "/${Formatter.formatShortFileSize(context, coilCustomDiskCache.toLong() * 1000 * 1000)}" +
                            stringResource(R.string.used)
                    else -> Formatter.formatShortFileSize(context, diskCacheSize) +
                            stringResource(R.string.used) +
                            " (${diskCacheSize * 100 / coilDiskCacheMaxSize.bytes}%)"
                },
                trailingContent = {
                    HeaderIconButton(
                        icon = R.drawable.trash,
                        enabled = true,
                        color = colorPalette().text,
                        onClick = { cleanCacheImages = true }
                    )
                },
                selectedValue = coilDiskCacheMaxSize,
                onValueSelected = {
                    coilDiskCacheMaxSize = it
                    if (coilDiskCacheMaxSize == CoilDiskCacheMaxSize.Custom)
                        showCoilCustomDiskCacheDialog = true

                    restartService = true
                },
                valueText = {
                    when (it) {
                        CoilDiskCacheMaxSize.Custom -> stringResource(R.string.custom)
                        CoilDiskCacheMaxSize.`32MB` -> "32MB"
                        CoilDiskCacheMaxSize.`64MB` -> "64MB"
                        CoilDiskCacheMaxSize.`128MB` -> "128MB"
                        CoilDiskCacheMaxSize.`256MB`-> "256MB"
                        CoilDiskCacheMaxSize.`512MB`-> "512MB"
                        CoilDiskCacheMaxSize.`1GB`-> "1GB"
                        CoilDiskCacheMaxSize.`2GB` -> "2GB"
                        CoilDiskCacheMaxSize.`4GB` -> "4GB"
                        CoilDiskCacheMaxSize.`8GB` -> "8GB"
                    }
                }
            )
            RestartPlayerService(restartService, onRestart = { restartService = false } )

            if (showCoilCustomDiskCacheDialog) {
                InputNumericDialog(
                    title = stringResource(R.string.set_custom_cache),
                    placeholder = stringResource(R.string.enter_value_in_mb),
                    value = coilCustomDiskCache.toString(),
                    valueMin = "32",
                    valueMax = "10000",
                    onDismiss = { showCoilCustomDiskCacheDialog = false },
                    setValue = {
                        //Log.d("customCache", it)
                        coilCustomDiskCache = it.toInt()
                        showCoilCustomDiskCacheDialog = false
                        restartService = true
                    }
                )
                RestartPlayerService(restartService, onRestart = { restartService = false } )
            }

            CacheSpaceIndicator(cacheType = CacheType.Images, horizontalPadding = 20.dp)
        }

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = stringResource(R.string.title_backup_and_restore))

        SettingsEntry(
            isEnabled = backupUiState is BackupUiState.Idle,
            title = stringResource(R.string.save_to_backup),
            text = stringResource(R.string.export_the_database),
            onClick = {
                isExporting = true
            }
        )
        SettingsDescription(text = stringResource(R.string.personal_preference))
        if (backupUiState is BackupUiState.BackingUp || backupUiState is BackupUiState.Restoring) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (backupUiState is BackupUiState.BackingUp) "Backup in progress..." else "Restore in progress..."
            )
        }

        SettingsEntry(
            isEnabled = backupUiState is BackupUiState.Idle,
            title = stringResource(R.string.restore_from_backup),
            text = stringResource(R.string.import_the_database),
            onClick = {
                //restoreFromOtherFileExtension = false
                isImporting = true
            }
        )
//        SettingsEntry(
//            title = stringResource(R.string.restore_from_other_backup),
//            text = stringResource(R.string.import_the_database_be_carefull),
//            onClick = {
//                restoreFromOtherFileExtension = true
//                isImporting = true
//            }
//        )
        ImportantSettingsDescription(text = stringResource(
            R.string.existing_data_will_be_overwritten,
            context.applicationInfo.nonLocalizedLabel
        ))

//        SettingsGroupSpacer()
//        SettingsEntryGroupText(title = stringResource(R.string.search_history))
//
//        SwitchSettingEntry(
//            title = stringResource(R.string.pause_search_history),
//            text = stringResource(R.string.neither_save_new_searched_query),
//            isChecked = pauseSearchHistory,
//            onCheckedChange = {
//                pauseSearchHistory = it
//                restartService = true
//            }
//        )
//        RestartPlayerService(restartService, onRestart = { restartService = false } )
//
//        SettingsEntry(
//            title = stringResource(R.string.clear_search_history),
//            text = if (queriesCount > 0) {
//                "${stringResource(R.string.delete)} " + queriesCount + stringResource(R.string.search_queries)
//            } else {
//                stringResource(R.string.history_is_empty)
//            },
//            isEnabled = queriesCount > 0,
//            onClick = { Database.asyncTransaction( Database::clearQueries ) }
//        )

//        SettingsGroupSpacer()
//        SettingsEntryGroupText(title = stringResource(R.string.playback_events))
//
//        SettingsEntry(
//            offline = false,
//            title = stringResource(R.string.reset_playback_events),
//            text = if (eventsCount > 0) {
//                stringResource(R.string.delete_playback_events, eventsCount)
//            } else {
//                stringResource(R.string.no_playback_events)
//            },
//            isEnabled = eventsCount > 0,
//            onClick = { clearEvents = true }
//        )

        SettingsGroupSpacer(
            modifier = Modifier.height(Dimensions.bottomSpacer)
        )
    }
}

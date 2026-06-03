package it.fast4x.riplay.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.extensions.preferences.PreferenceKey
import it.fast4x.riplay.musicvault.MusicVaultDisclaimerDialog
import it.fast4x.riplay.musicvault.MusicVaultFolderSetting
import it.fast4x.riplay.musicvault.checkAndStartMusicVault
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.settingsItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.LocalAppearance
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DEFAULT_FOLDER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.LOG_DEBUG_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MUSIC_VAULT_DISCLAIMER_ACCEPTED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MUSIC_VAULT_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NAVIGATION_BAR_POSITION
import it.fast4x.riplay.extensions.preferences.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("BatteryLife")
@ExperimentalAnimationApi
@Composable
fun MiscSettings() {
    val context = LocalContext.current
    val (colorPalette, _, _) = LocalAppearance.current
    var defaultFolder by rememberPreference(DEFAULT_FOLDER.key, "/")
    val navigationBarPosition by rememberPreference(
        NAVIGATION_BAR_POSITION.key,
        NavigationBarPosition.Bottom
    )

    var logDebugEnabled by rememberPreference(LOG_DEBUG_ENABLED.key, false)

    var fileName by remember {
        mutableStateOf("")
    }

    var text by remember { mutableStateOf(null as String?) }

    val noLogAvailable = stringResource(R.string.no_log_available)
    var exportCrashlog by remember{ mutableStateOf(false) }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            val file =
                File(context.filesDir.resolve("logs"),
                    if (exportCrashlog) "RiPlay_crash_log.txt" else  "RiPlay_log.txt"
                )
            if (file.exists()) {
                text = file.readText()
            } else {
                SmartMessage(noLogAvailable, type = PopupType.Info, context = context)
                return@rememberLauncherForActivityResult
            }

            context.applicationContext.contentResolver.openOutputStream(uri)
                ?.use { outputStream ->
                    FileInputStream( file ).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

        }

    var isExporting by rememberSaveable {
        mutableStateOf(false)
    }


    if (isExporting) {
        InputTextDialog(
            onDismiss = {
                isExporting = false
            },
            title = stringResource(R.string.enter_the_name_of_log_export),
            value = "",
            placeholder = stringResource(R.string.enter_the_name_of_log_export),
            setValue = { txt ->
                fileName = txt
                try {
                    @SuppressLint("SimpleDateFormat")
                    val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                    exportLauncher.launch("RMLog_${txt.take(20)}_${dateFormat.format(
                        Date()
                    )}")
                } catch (e: ActivityNotFoundException) {
                    SmartMessage("Couldn't find an application to create documents",
                        type = PopupType.Warning, context = context)
                }
            }
        )
    }

    var musicVaultEnabled by rememberPreference(MUSIC_VAULT_ENABLED.key, false)
    var disclaimerAccepted by rememberPreference(MUSIC_VAULT_DISCLAIMER_ACCEPTED.key, false)
    var showDisclaimer by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if (navigationBarPosition == NavigationBarPosition.Left ||
                    navigationBarPosition == NavigationBarPosition.Top ||
                    navigationBarPosition == NavigationBarPosition.Bottom
                ) 1f
                else Dimensions.contentWidthRightBar
            )
    ) {
        LazyColumn(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(bottom = Dimensions.bottomSpacer)
        ) {
            settingsItem {
                HeaderWithIcon(
                    title = stringResource(R.string.tab_miscellaneous),
                    iconId = R.drawable.equalizer,
                    enabled = false,
                    showIcon = true,
                    modifier = Modifier,
                    onClick = {}
                )
            }

            if (BuildConfig.FLAVOR == "full") {
                settingsItem(
                    isHeader = true
                ) {
                    SettingsGroupSpacer()
                    SettingsEntryGroupText(title = stringResource(R.string.settings_music_vault_title))
                }
                settingsItem {
                    SwitchSettingEntry(
                        title = stringResource(R.string.settings_music_vault_enable_personal_audio_saving),
                        text = stringResource(R.string.settings_music_vault_save_songs_from_youtube_for_personal_offline_listening),
                        isChecked = musicVaultEnabled,
                        onCheckedChange = {
                            if (it) {
                                if (disclaimerAccepted) musicVaultEnabled = true
                                else showDisclaimer = true
                            } else {
                                musicVaultEnabled = false
                                disclaimerAccepted = false
                            }
                        }
                    )

                    // Disclaimer dialog
                    if (showDisclaimer) {
                        MusicVaultDisclaimerDialog(
                            onAccept = {
                                disclaimerAccepted = true
                                musicVaultEnabled = true
                                showDisclaimer = false
                                CoroutineScope(Dispatchers.IO).launch {
                                    // Disclaimer accettato quindi Music Vault può essere avviato
                                    checkAndStartMusicVault()
                                }
                            },
                            onDecline = {
                                showDisclaimer = false
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = musicVaultEnabled && disclaimerAccepted
                    ) {
                        MusicVaultFolderSetting()
                    }
                }
            }

            settingsItem(
                isHeader = true
            ) {
                SettingsGroupSpacer()
                SettingsEntryGroupText(title = stringResource(R.string.settings_title_on_device_music_folder))
            }

            settingsItem {
                FolderSetting(PreferenceKey.DEFAULT_FOLDER, "")
            }


            settingsItem(
                isHeader = true
            ) {
                SettingsGroupSpacer()
                SettingsEntryGroupText(title = stringResource(R.string.debug))
            }

            settingsItem {
                SwitchSettingEntry(
                    title = stringResource(R.string.enable_log_debug),
                    text = stringResource(R.string.if_enabled_create_a_log_file_to_highlight_errors),
                    isChecked = logDebugEnabled,
                    onCheckedChange = {
                        logDebugEnabled = it
                        if (!it) {
                            val file = File(context.filesDir.resolve("logs"), "RiPlay_log.txt")
                            if (file.exists())
                                file.delete()

                            val filec =
                                File(context.filesDir.resolve("logs"), "RiPlay_crash_log.txt")
                            if (filec.exists())
                                filec.delete()


                        } else
                            SmartMessage(
                                context.resources.getString(R.string.restarting_riplay_is_required),
                                type = PopupType.Info, context = context
                            )
                    }
                )
                ImportantSettingsDescription(text = stringResource(R.string.restarting_riplay_is_required))
                ButtonBarSettingEntry(
                    isEnabled = logDebugEnabled,
                    title = stringResource(R.string.export_log),
                    text = "",
                    icon = R.drawable.export,
                    onClick = {
                        exportCrashlog = false
                        isExporting = true
                    }
                )
                ButtonBarSettingEntry(
                    title = stringResource(R.string.export_crash_log),
                    text = stringResource(R.string.is_always_enabled),
                    icon = R.drawable.export,
                    onClick = {
                        exportCrashlog = true
                        isExporting = true
                    }
                )
            }
        }

    }
}

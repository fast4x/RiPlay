package it.fast4x.riplay.ui.screens.settings

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import it.fast4x.environment.Environment
import it.fast4x.environment.models.responses.CachedAccountProfile
import it.fast4x.environment.utils.parseCookieString
import it.fast4x.riplay.Dependencies
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.LocalAudioTagger
import it.fast4x.riplay.LocalWebDavLibrary
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.webDavAccountEmpty
import it.fast4x.riplay.enums.MusicIdentifierProvider
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.globalContext
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.ValidationType
import it.fast4x.riplay.extensions.discord.DiscordLoginAndGetToken
import it.fast4x.riplay.extensions.accountlogin.AccountLogin
import it.fast4x.riplay.extensions.webdavlibrary.models.WebDavBackupInfo
import it.fast4x.riplay.extensions.webdavlibrary.models.WebDavBrowserState
import it.fast4x.riplay.extensions.webdavlibrary.ui.WebDavAccountFormSheet
import it.fast4x.riplay.extensions.webdavlibrary.ui.WebDavDatabaseRestoreConfirmDialog
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.utils.isAtLeastAndroid81
import it.fast4x.riplay.ui.components.themed.AccountInfoDialog
import it.fast4x.riplay.extensions.lastfm.LastFmAuthScreen
import it.fast4x.riplay.extensions.listenbrainz.ListenBrainzAuthScreen
import it.fast4x.riplay.extensions.smoothloader.Loader
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber

@UnstableApi
@DelicateCoroutinesApi
@ExperimentalMaterial3Api
@SuppressLint("BatteryLife")
@ExperimentalAnimationApi
@Composable
fun AccountsSettings(
    navController: NavController
) {
    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val context = LocalContext.current
    val search = Search.init(alwaysVisible = true)

    val searchWebdavVisible = search.matchesAny(R.string.settings_webdav_title, R.string.settings_webdav_link_cloud_and_music_title, R.string.settings_webdav_link_cloud_and_music_subtitle, R.string.settings_webdav_cloud_accounts, R.string.settings_webdav_link_music_title, R.string.settings_webdav_link_music_subtitle, R.string.settings_webdav_backup_title, R.string.settings_webdav_backup_subtitle, R.string.settings_webdav_restore_title, R.string.settings_webdav_restore_subtitle)
    val thumbnailRoundness = appearanceSettings.thumbnailRoundness

    var showUserInfoDialog by rememberSaveable { mutableStateOf(false) }
    val isEnabledMusicIdentifier = appSettings.enableMusicIdentifier

    val musicIdentifierProvider = appSettings.musicIdentifierProvider

    val musicIdentifierApi = appSettings.musicIdentifierApi

    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .background(colorPalette().background0)
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
            title = stringResource(R.string.tab_accounts),
            iconId = R.drawable.person,
            enabled = false,
            showIcon = true,
            modifier = Modifier,
            onClick = {},
            navController = navController
        )

        search.SearchBar(this)




        /****** WEBDAV ******/
        var webDavSync by remember { mutableStateOf(false) }
        var showWebDavAccount by remember { mutableStateOf(false) }
        var webDavAccountEditing by remember { mutableStateOf(false) }
        val isWebDavEnabled = appSettings.isWebDavEnabled
        val webdavViewModel = LocalWebDavLibrary.current
        val webdavUiState by webdavViewModel.uiState.collectAsStateWithLifecycle()
        val webdavAccounts = webdavViewModel.accounts.collectAsStateWithLifecycle()
        val webdavAsMusicSource = webdavAccounts.value.any { it.isMusicSource }
        val webdavAsBackup = webdavAccounts.value.any { !it.isMusicSource }
        val testConnectionState by webdavViewModel.testConnectionState.collectAsStateWithLifecycle()
        var accountSelected by remember { mutableStateOf(webdavAccounts.value.firstOrNull() ?: webDavAccountEmpty() )}
        var isFetchingRestore by remember { mutableStateOf(false) }
        var backupInfo by remember { mutableStateOf<WebDavBackupInfo?>(null) }
        var isRestoring by remember { mutableStateOf(false) }

        key(webDavAccountEditing) {
            WebDavAccountFormSheet(
                showSheet = showWebDavAccount,
                isEditing = webDavAccountEditing,
                account = if (webDavAccountEditing) accountSelected else webDavAccountEmpty(),
                testState = testConnectionState,
                onTestConnection = { account ->
                    webdavViewModel.testConnection(account)
                },
                onSave = { account ->
                    webdavViewModel.saveAccount(account)
                    showWebDavAccount = false
                    webDavAccountEditing = false
                },
            ) {
                showWebDavAccount = false
                webdavViewModel.resetTestState()
            }
        }


        if (searchWebdavVisible) {
            SettingsGroupSpacer()
            SettingsEntryGroupText(title = stringResource(R.string.settings_webdav_title))
        }

        LaunchedEffect(webDavSync) {
            if (webDavSync) {
                webdavViewModel.loadAllMusicFolders()
                webDavSync = false
            }
        }

        when (val state = webdavUiState) {
            is WebDavBrowserState.Idle -> {
                Timber.d("AccountsSettings WebDAV: Idle")
            }
            is WebDavBrowserState.Loading -> {
                Timber.d("AccountsSettings WebDAV: Loading...")
                Loader()
            }

            is WebDavBrowserState.Success -> {
                Timber.d("AccountsSettings WebDAV: Success folders = ${state.folders.size} songs count = ${state.songs.size} songs urls = ${state.songs.map { it.id }}")
                SmartMessage("WebDAV Success: songs = ${state.songs.size}", context = context)
            }

            is WebDavBrowserState.Error -> {
                Timber.e("AccountsSettings WebDAV: Error message = ${state.message}")
                SmartMessage("WebDAV Error: ${state.message}", context = context)
            }
        }

        if (searchWebdavVisible) {
    SettingsCardContainer {
                SwitchSettingEntry(
                    title = stringResource(R.string.settings_webdav_link_cloud_and_music_title),
                    text = stringResource(R.string.settings_webdav_link_cloud_and_music_subtitle),
                    isChecked = isWebDavEnabled,
                    onCheckedChange = {
                        coroutineScope.launch {
                            val new = appSettingsManager.activeSettings.value.copy(isWebDavEnabled = it)
                            appSettingsManager.updateSettings(new)
                        }
                    }, icon = R.drawable.link,
                )

                AnimatedVisibility(visible = isWebDavEnabled) {
                    Column {

                        key(webdavAccounts.value) {
                            ValueWebDavAccountSelectorSettingsEntry(
                                title = stringResource(R.string.settings_webdav_cloud_accounts),
                                titleSecondary = "",
                                values = webdavAccounts.value,
                                onValueSelected = { account ->
                                    accountSelected = account
                                    webDavAccountEditing = true
                                    showWebDavAccount = true
                                },
                                onNewAccount = {
                                    webDavAccountEditing = false
                                    showWebDavAccount = true
                                },
                                onDismiss = { webdavViewModel.refreshAccounts() }, icon = R.drawable.cloud,
                            )
                        }


                        AnimatedVisibility(visible = webdavAccounts.value.isNotEmpty()) {
                            Column {
                                if (webdavAsMusicSource)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ButtonBarSettingEntry(
                                            isEnabled = true,
                                            title = stringResource(R.string.settings_webdav_link_music_title),
                                            text = stringResource(R.string.settings_webdav_link_music_subtitle),
                                            icon = Icons.Rounded.CloudSync,
                                            iconColor = colorPalette().text,
                                            onClick = {
                                                webDavSync = true
                                            },
                                        )
                                    }

                                if (webdavAsBackup) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ButtonBarSettingEntry(
                                            isEnabled = true,
                                            title = stringResource(R.string.settings_webdav_backup_title),
                                            text = stringResource(R.string.settings_webdav_backup_subtitle),
                                            icon = Icons.Rounded.CloudUpload,
                                            iconColor = colorPalette().text,
                                            onClick = {
                                                webdavViewModel.syncDatabaseToWebDav(Dependencies.application)
                                            }
                                        )
                                    }


                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ButtonBarSettingEntry(
                                            isEnabled = true,
                                            title = stringResource(R.string.settings_webdav_restore_title),
                                            text = stringResource(R.string.settings_webdav_restore_subtitle),
                                            icon = Icons.Rounded.CloudDownload,
                                            iconColor = colorPalette().text,
                                            onClick = {
                                                val appScope = Dependencies.application.appScopeIO
                                                isFetchingRestore = true
                                                webdavAccounts.value.fastFilter { !it.isMusicSource }
                                                    .firstOrNull()?.let { webDavAccount ->

                                                        // Lancia la coroutine nel ViewModel per scaricare il riplay_meta.json
                                                        appScope.launch {
                                                            backupInfo = webdavViewModel.fetchBackupInfo(
                                                                webDavAccount
                                                            )
                                                            isFetchingRestore = false
                                                        }
                                                    }
                                            },
                                        )
                                        // Mostra un loader se stai scaricando le info
                                        if (isFetchingRestore) {
                                            CircularProgressIndicator()
                                        }
                                        // Mostra il Dialog del restore se le info sono pronte
                                        backupInfo?.let { info ->
                                            WebDavDatabaseRestoreConfirmDialog(
                                                backupInfo = info,
                                                onConfirm = {
                                                    backupInfo = null // Chiude il dialog
                                                    isRestoring = true
                                                    // Lancia il restore vero e proprio
                                                    val appScope = Dependencies.application.appScopeIO
                                                    appScope.launch {
                                                        webdavAccounts.value.fastFilter { !it.isMusicSource }
                                                            .firstOrNull()?.let { webDavAccount ->
                                                                webdavViewModel.syncDatabaseFromWebDav(
                                                                    Dependencies.application,
                                                                    webDavAccount
                                                                )
                                                            }
                                                    }
                                                },
                                                onDismiss = {
                                                    backupInfo = null // Chiude il dialog senza fare nulla
                                                }
                                            )
                                        }

                                    }
                                }
                            }


                        }
                    }
                }
            }
        }

        /****** WEBDAV ******/

        /****** YOUTUBE LOGIN ******/

        val isYouTubeLoginEnabled = appSettings.enableYtLogin
        val isSyncEnabled = appSettings.enableYtSync
        var loginYouTube by remember { mutableStateOf(false) }
        val cookie = appSettings.ytCookie
        val accountName = appSettings.ytAccountName
        val accountEmail = appSettings.ytAccountEmail

        val accountChannelHandle = appSettings.ytAccountChannelHandle
        val accountThumbnail = appSettings.ytAccountThumbnail

        val jsonCachedAccounts = appSettings.ytCachedAccounts
        var cachedAccounts = try {
            Json.decodeFromString<List<CachedAccountProfile>>(jsonCachedAccounts)
        } catch (e: Exception) {
            Timber.e(e, "Errore nel parsing della cache account")
            emptyList()
        }

        if (search.matchesAny(R.string.title_youtube_music, R.string.enable_youtube_music_login, R.string.disconnect, R.string.connect, R.string.login_connect_or_disconnect_your_account, R.string.login_switch_account, R.string.login_info_you_can_switch_to_another_account_without_completely_disconnecting, R.string.account_info, R.string.login_info_you_can_quickly_check_which_account_you_are_connected_to, R.string.sync_data_with_ytm_account, R.string.sync_data_playlists_albums_artists_history_like_etc)) {
            SettingsGroupSpacer()
            SettingsEntryGroupText(title = stringResource(R.string.title_youtube_music))

            SettingsCardContainer {
                SwitchSettingEntry(
                    title = stringResource(R.string.enable_youtube_music_login),
                    text = "",
                    isChecked = isYouTubeLoginEnabled,
                    onCheckedChange = {
                        coroutineScope.launch {
                            val new = appSettingsManager.activeSettings.value.copy(enableYtLogin = it)
                            appSettingsManager.updateSettings(new)
                            // Usiamo solo se quando l'utente disattiva vogliamo rimuovere i dati di login
        //                    if (!it) {
        //                        appSettingsManager.updateSettings(
        //                            new.copy(
        //                                ytAccountName = "",
        //                                ytAccountChannelHandle = "",
        //                                ytAccountThumbnail = ""
        //                            )
        //                        )
        //                    }
                        }

                    }, icon = R.drawable.switch_user,
                )

                AnimatedVisibility(visible = isYouTubeLoginEnabled) {
                    Column {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween

                            ){

                                if (isYtLoggedIn() && accountThumbnail != "")
                                    AsyncImage(
                                        model = accountThumbnail,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .height(45.dp)
                                            .clip(thumbnailShape())
                                    )

                                Column {
                                    ButtonBarSettingEntry(
                                        isEnabled = true,
                                        title = if (isYtLoggedIn()) stringResource(R.string.disconnect) else stringResource(
                                            R.string.connect
                                        ),
                                        text = stringResource(R.string.login_connect_or_disconnect_your_account),
                                        icon = R.drawable.internet,
                                        iconColor = colorPalette().text,
                                        onClick = {
                                            if (isYtLoggedIn()) {
                                                coroutineScope.launch {
                                                    val new = appSettingsManager.activeSettings.value.copy(
                                                        ytCookie = "",
                                                        ytAccountName = "",
                                                        ytAccountChannelHandle = "",
                                                        ytAccountEmail = "",
                                                        ytAccountThumbnail = "",
                                                        ytCachedAccounts = ""
                                                    )
                                                    appSettingsManager.updateSettings(new)
                                                }

                                                coroutineScope.launch {
                                                    // Eliminiamo i non riproducibili contrassegnati quando l'utente era autenticato
                                                    Database.unplayableTracksDao().revokeForLoginState(true)
                                                }

                                                cachedAccounts = emptyList()
                                                loginYouTube = false
                                                val cookieManager = CookieManager.getInstance()
                                                cookieManager.removeAllCookies(null)
                                                cookieManager.flush()
                                                WebStorage.getInstance().deleteAllData()
                                            } else {
                                                coroutineScope.launch {
                                                    // Eliminiamo i non riproducibili contrassegnati quando l'utente NON era autenticato
                                                    Database.unplayableTracksDao().revokeForLoginState(false)
                                                }
                                                loginYouTube = true
                                            }
                                        }
                                    )

                                    if (isYtLoggedIn()) {

                                        if (cachedAccounts.isNotEmpty())
                                            ButtonBarSettingEntry(
                                                isEnabled = true,
                                                title = stringResource(R.string.login_switch_account),
                                                text = stringResource(R.string.login_info_you_can_switch_to_another_account_without_completely_disconnecting),
                                                icon = R.drawable.switch_user,
                                                iconColor = colorPalette().text,
                                                onClick = {
                                                    loginYouTube = true
                                                }
                                            )

                                        ButtonBarSettingEntry(
                                            isEnabled = true,
                                            title = stringResource(R.string.account_info),
                                            text = stringResource(R.string.login_info_you_can_quickly_check_which_account_you_are_connected_to),
                                            icon = R.drawable.person,
                                            iconColor = colorPalette().text,
                                            onClick = {
                                                if (accountThumbnail == "" || accountName == "" || accountEmail == "")
                                                    GlobalScope.launch {
                                                        Environment.accountInfo().onSuccess {
                                                            val new = appSettingsManager.activeSettings.value.copy(
                                                                ytAccountName = it?.name.orEmpty(),
                                                                ytAccountEmail = it?.email.orEmpty(),
                                                                ytAccountChannelHandle =
                                                                    it?.channelHandle.orEmpty(),
                                                                ytAccountThumbnail = it?.thumbnailUrl.orEmpty()
                                                            )
                                                            appSettingsManager.updateSettings(new)
                                                        }.onFailure {
                                                            Timber.e("Error YoutubeLogin: $it.stackTraceToString()")
                                                        }
                                                    }
                                                showUserInfoDialog = true
                                            }
                                        )

                                        SwitchSettingEntry(
                                            title = stringResource(R.string.sync_data_with_ytm_account),
                                            text = stringResource(R.string.sync_data_playlists_albums_artists_history_like_etc),
                                            isChecked = isSyncEnabled,
                                            onCheckedChange = {
                                                coroutineScope.launch {
                                                    val new = appSettingsManager.activeSettings.value.copy(enableYtSync = it)
                                                    appSettingsManager.updateSettings(new)
                                                }
                                            }, icon = R.drawable.sync,
                                        )
                                    }


                                    CustomModalBottomSheet(
                                        showSheet = loginYouTube,
                                        onDismissRequest = {
                                            loginYouTube = false
                                        },
                                        containerColor = colorPalette().background0,
                                        contentColor = colorPalette().background0,
                                        modifier = Modifier.fillMaxWidth(),
                                        dragHandle = {
                                            Surface(
                                                modifier = Modifier.padding(vertical = 0.dp),
                                                color = colorPalette().background0,
                                                shape = thumbnailShape()
                                            ) {}
                                        },
                                        shape = thumbnailRoundness.shape()
                                    ) {
                                        AccountLogin(onLogin = { loginYouTube = false })
                                    }

                                }

                            }


                    }
                }
            }
        }

        if (showUserInfoDialog) {
            AccountInfoDialog(
                accountName = accountName,
                accountEmail = accountEmail,
                accountChannelHandle = accountChannelHandle,
                onDismiss = { showUserInfoDialog = false }
            )
        }

    /****** YOUTUBE LOGIN ******/



        /****** LASTFM ******/
        val isEnabledLastfm = appSettings.isEnabledLastFM
        val lastFmSessionToken = appSettings.lastFMSessionToken
        var loginLastfm by remember { mutableStateOf(false) }

        val lastfmScrobbleType = appSettings.lastFmScrobbleType

        if (search.matchesAny(R.string.title_lastfm, R.string.enable_lastfm, R.string.lastfm_disconnect, R.string.lastfm_connect, R.string.lastfm_connected_to_lastfm_account, R.string.lastfm_scrobble_type)) {
            SettingsGroupSpacer()
            SettingsEntryGroupText(title = stringResource(R.string.title_lastfm))

            SettingsCardContainer {
                SwitchSettingEntry(
                    title = stringResource(R.string.enable_lastfm),
                    text = "",
                    isChecked = isEnabledLastfm,
                    onCheckedChange = {
                        coroutineScope.launch {
                            val new = appSettingsManager.activeSettings.value.copy(isEnabledLastFM = it)
                            appSettingsManager.updateSettings(new)
                        }
                    }, icon = R.drawable.logo_lastfm,
                )

                AnimatedVisibility(visible = isEnabledLastfm) {
                    Column {
                        ButtonBarSettingEntry(
                            isEnabled = true,
                            title = if (lastFmSessionToken.isNotEmpty()) stringResource(R.string.lastfm_disconnect) else stringResource(
                                R.string.lastfm_connect
                            ),
                            text = if (lastFmSessionToken.isNotEmpty()) stringResource(R.string.lastfm_connected_to_lastfm_account) else "",
                            icon = R.drawable.logo_lastfm,
                            iconColor = colorPalette().text,
                            onClick = {
                                if (lastFmSessionToken.isNotEmpty()) {
                                    coroutineScope.launch {
                                        val new = appSettingsManager.activeSettings.value.copy(lastFMSessionToken = "")
                                        appSettingsManager.updateSettings(new)
                                    }
                                } else
                                    loginLastfm = true
                            }
                        )

                        CustomModalBottomSheet(
                            showSheet = loginLastfm,
                            onDismissRequest = {
                                loginLastfm = false
                            },
                            containerColor = colorPalette().background0,
                            contentColor = colorPalette().background0,
                            modifier = Modifier.fillMaxWidth(),
                            dragHandle = {
                                Surface(
                                    modifier = Modifier.padding(vertical = 0.dp),
                                    color = colorPalette().background0,
                                    shape = thumbnailShape()
                                ) {}
                            },
                            shape = thumbnailRoundness.shape()
                        ) {
                            LastFmAuthScreen(
                                navController = rememberNavController(),
                                onAuthSuccess = {
                                    loginLastfm = false
                                    // controllare se necessario
                                    //lastFmSessionToken = appSettings.lastFMSessionToken
                                    //context.preferences.getString(LASTFM_SESSION_TOKEN.key, "") ?: ""
                                    Timber.d("LastFmAuthScreen: Authentication complete")
                                }
                            )
                        }

                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.lastfm_scrobble_type),
                            titleSecondary = "",
                            selectedValue = lastfmScrobbleType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(lastFmScrobbleType = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { it.textName }, icon = R.drawable.logo_lastfm,
                        )

                    }
                }
            }
        }

        /****** LASTFM ******/

        /****** LISTENBRAINZ ******/
        val isEnabledListenBrainz = appSettings.isEnabledListenBrainz
        val listenBrainzToken = appSettings.listenBrainzToken
        val listenBrainzUsername = appSettings.listenBrainzUsername
        val listenBrainzServerUrl = appSettings.listenBrainzServerUrl
        var loginListenBrainz by remember { mutableStateOf(false) }

        val listenBrainzScrobbleType = appSettings.listenBrainzScrobbleType

        if (search.matchesAny(R.string.title_listenbrainz, R.string.enable_listenbrainz, R.string.listenbrainz_disconnect, R.string.listenbrainz_connect, R.string.listenbrainz_connected_to_listenbrainz_account, R.string.listenbrainz_scrobble_type, R.string.listenbrainz_server_url)) {
            SettingsGroupSpacer()
            SettingsEntryGroupText(title = stringResource(R.string.title_listenbrainz))

            SettingsCardContainer {
                SwitchSettingEntry(
                    title = stringResource(R.string.enable_listenbrainz),
                    text = "",
                    isChecked = isEnabledListenBrainz,
                    onCheckedChange = {
                        coroutineScope.launch {
                            val new = appSettingsManager.activeSettings.value.copy(isEnabledListenBrainz = it)
                            appSettingsManager.updateSettings(new)
                        }
                    }, icon = R.drawable.logo_listenbrainz,
                )

                AnimatedVisibility(visible = isEnabledListenBrainz) {
                    Column {
                        ButtonBarSettingEntry(
                            isEnabled = true,
                            title = if (listenBrainzToken.isNotEmpty()) stringResource(R.string.listenbrainz_disconnect) else stringResource(
                                R.string.listenbrainz_connect
                            ),
                            text = if (listenBrainzToken.isNotEmpty()) stringResource(R.string.listenbrainz_connected_to_listenbrainz_account) else "",
                            icon = R.drawable.logo_listenbrainz,
                            iconColor = colorPalette().text,
                            onClick = {
                                if (listenBrainzToken.isNotEmpty()) {
                                    coroutineScope.launch {
                                        val new = appSettingsManager.activeSettings.value.copy(
                                            listenBrainzToken = "",
                                            listenBrainzUsername = ""
                                        )
                                        appSettingsManager.updateSettings(new)
                                    }
                                } else
                                    loginListenBrainz = true
                            }
                        )

                        if (listenBrainzToken.isNotEmpty() && listenBrainzUsername.isNotEmpty()) {
                            ButtonBarSettingEntry(
                                isEnabled = true,
                                title = stringResource(R.string.account_info),
                                text = listenBrainzUsername,
                                icon = R.drawable.person,
                                iconColor = colorPalette().text,
                                onClick = { }
                            )
                        }

                        // Server ListenBrainz: vuoto = server ufficiale, altrimenti istanza self-hosted
                        TextDialogSettingEntry(
                            title = stringResource(R.string.listenbrainz_server_url),
                            text = listenBrainzServerUrl.ifEmpty { stringResource(R.string.listenbrainz_server_url_official) },
                            currentText = listenBrainzServerUrl,
                            onTextSave = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(
                                        listenBrainzServerUrl = it.trim()
                                    )
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            validationType = ValidationType.Url,
                            icon = R.drawable.logo_listenbrainz,
                        )

                        CustomModalBottomSheet(
                            showSheet = loginListenBrainz,
                            onDismissRequest = {
                                loginListenBrainz = false
                            },
                            containerColor = colorPalette().background0,
                            contentColor = colorPalette().text,
                            modifier = Modifier.fillMaxWidth(),
                            dragHandle = {
                                Surface(
                                    modifier = Modifier.padding(vertical = 0.dp),
                                    color = colorPalette().background0,
                                    shape = thumbnailShape()
                                ) {}
                            },
                            shape = thumbnailRoundness.shape()
                        ) {
                            ListenBrainzAuthScreen(
                                onAuthSuccess = {
                                    loginListenBrainz = false
                                    Timber.d("ListenBrainzAuthScreen: Authentication complete")
                                }
                            )
                        }

                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.listenbrainz_scrobble_type),
                            titleSecondary = "",
                            selectedValue = listenBrainzScrobbleType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(listenBrainzScrobbleType = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { it.textName }, icon = R.drawable.logo_listenbrainz,
                        )

                    }
                }
            }
        }

        /****** LISTENBRAINZ ******/

        /****** DISCORD ******/
        val isDiscordPresenceEnabled = appSettings.isDiscordPresenceEnabled
        var loginDiscord by remember { mutableStateOf(false) }
        var showDiscordUserInfoDialog by remember { mutableStateOf(false) }

        val discordPersonalAccessToken = appSettings.discordPersonalAccessToken

        val discordAccountName = appSettings.discordAccountName

        if (search.matchesAny(R.string.social_discord, R.string.discord_enable_rich_presence, R.string.discord_disconnect, R.string.discord_connect, R.string.discord_connected_to_discord_account, R.string.account_info)) {
            SettingsGroupSpacer()
            SettingsEntryGroupText(title = stringResource(R.string.social_discord))
            SettingsCardContainer {
                SwitchSettingEntry(
                    isEnabled = isAtLeastAndroid81,
                    title = stringResource(R.string.discord_enable_rich_presence),
                    text = "",
                    isChecked = isDiscordPresenceEnabled,
                    onCheckedChange = {
                        coroutineScope.launch {
                            val new = appSettingsManager.activeSettings.value.copy(isDiscordPresenceEnabled = it)
                            appSettingsManager.updateSettings(new)
                        }
                    }, icon = R.drawable.logo_discord,
                )

                AnimatedVisibility(visible = isDiscordPresenceEnabled) {
                    Column {
                        ButtonBarSettingEntry(
                            isEnabled = true,
                            title = if (discordPersonalAccessToken.isNotEmpty()) stringResource(R.string.discord_disconnect) else stringResource(
                                R.string.discord_connect
                            ),
                            text = if (discordPersonalAccessToken.isNotEmpty()) stringResource(R.string.discord_connected_to_discord_account) else "",
                            icon = R.drawable.logo_discord,
                            iconColor = colorPalette().text,
                            onClick = {
                                if (discordPersonalAccessToken.isNotEmpty()) {
                                    coroutineScope.launch {
                                        val new = appSettingsManager.activeSettings.value.copy(discordPersonalAccessToken = "")
                                        appSettingsManager.updateSettings(new)
                                    }
                                } else
                                    loginDiscord = true
                            }
                        )

                        if (discordPersonalAccessToken.isNotEmpty()) {
                            ButtonBarSettingEntry(
                                isEnabled = true,
                                title = stringResource(R.string.account_info),
                                text = discordAccountName,
                                icon = R.drawable.person,
                                iconColor = colorPalette().text,
                                onClick = {
                                    showDiscordUserInfoDialog = true
                                }
                            )

                            if (showDiscordUserInfoDialog) {
                                AccountInfoDialog(
                                    accountName = discordAccountName,
                                    onDismiss = { showDiscordUserInfoDialog = false }
                                )
                            }

                        }

                        CustomModalBottomSheet(
                            showSheet = loginDiscord,
                            onDismissRequest = {
                                loginDiscord = false
                            },
                            containerColor = colorPalette().background0,
                            contentColor = colorPalette().background0,
                            modifier = Modifier.fillMaxWidth(),
                            dragHandle = {
                                Surface(
                                    modifier = Modifier.padding(vertical = 0.dp),
                                    color = colorPalette().background0,
                                    shape = thumbnailShape()
                                ) {}
                            },
                            shape = thumbnailRoundness.shape()
                        ) {
                            DiscordLoginAndGetToken(
                                navController = rememberNavController(),
                                onGetToken = { token, username, avatar ->
                                    //Timber.d("DiscordLoginAndGetToken DiscordPresence: token $token user $username avatar $avatar")
                                    loginDiscord = false
                                    coroutineScope.launch {
                                        val new = appSettingsManager.activeSettings.value.copy(
                                            discordPersonalAccessToken = token,
                                            discordAccountName = username,
                                        )
                                        appSettingsManager.updateSettings(new)
                                    }
                                    SmartMessage(
                                        globalContext().resources.getString(R.string.discord_connected_to_discord_account) + " $username",
                                        type = PopupType.Info,
                                        context = context
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }


        /****** DISCORD ******/

        /**** MUSIC IDENTIFIER ******/
        if (search.matchesAny(R.string.title_music_identifier, R.string.enable_music_identifier, R.string.music_identifier_provider, R.string.api_key, R.string.if_empty_system_api_key_will_be_used, R.string.api_expiration, R.string.api_queries_count, R.string.music_identifier_free_identification_seconds_remaining)) {
            SettingsGroupSpacer()
            SettingsEntryGroupText(title = stringResource(R.string.title_music_identifier))

            SettingsCardContainer {
                SwitchSettingEntry(
                    title = stringResource(R.string.enable_music_identifier),
                    text = "",
                    isChecked = isEnabledMusicIdentifier,
                    onCheckedChange = {
                        coroutineScope.launch {
                            val new = appSettingsManager.activeSettings.value.copy(enableMusicIdentifier = it)
                            appSettingsManager.updateSettings(new)
                        }
                    }, icon = R.drawable.mic,
                )

                AnimatedVisibility(visible = isEnabledMusicIdentifier) {
                    Column {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.music_identifier_provider),
                            titleSecondary = musicIdentifierProvider.info,
                            selectedValue = musicIdentifierProvider,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(musicIdentifierProvider = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { it.title }, icon = R.drawable.mic,
                        )
                        SettingsEntry(
                            title = musicIdentifierProvider.subtitle,
                            titleSecondary = musicIdentifierProvider.website,
                            text = "",
                            onClick = {
                                uriHandler.openUri(musicIdentifierProvider.website)
                            }
                        )


                        AnimatedVisibility(visible = musicIdentifierProvider == MusicIdentifierProvider.AudioTagInfo) {
                            Column {
                                TextDialogSettingEntry(
                                    title = stringResource(R.string.api_key),
                                    text = musicIdentifierApi.ifEmpty { stringResource(R.string.if_empty_system_api_key_will_be_used) },
                                    currentText = musicIdentifierApi,
                                    onTextSave = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(musicIdentifierApi = it)
                                            appSettingsManager.updateSettings(new)
                                        }
                                    },
                                    validationType = ValidationType.None, icon = R.drawable.shield_checkmark,
                                )

                                val localAudioTagger = LocalAudioTagger.current
                                LaunchedEffect(Unit) {
                                    localAudioTagger.stat()
                                }
                                val statState by localAudioTagger.statsState.collectAsState()
                                if (statState?.success == true) {

                                    BasicText(
                                        text = stringResource(
                                            R.string.api_expiration,
                                            statState?.expirationDate?.substring(0, 10).toString()
                                        ),
                                        style = typography().xxs.semiBold.copy(color = colorPalette().text),
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    BasicText(
                                        text = stringResource(
                                            R.string.api_queries_count,
                                            statState?.queriesCount ?: "0"

                                        ),
                                        style = typography().xxs.semiBold.copy(color = colorPalette().textSecondary),
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    BasicText(
                                        text = stringResource(
                                            R.string.music_identifier_free_identification_seconds_remaining,
                                            statState?.identificationFreeSecRemainder ?: "0"
                                        ),
                                        style = typography().xxs.semiBold.copy(color = colorPalette().textSecondary),
                                    )
                                    Spacer(
                                        modifier = Modifier
                                            .height(Dimensions.bottomSpacer)
                                    )
                                }
                            }

                        }
                    }
                }
            }
        }
        /**** MUSIC IDENTIFIER ******/

        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))

    }



}

fun isYtLoginEnabled(): Boolean {
    val appSettingsManager = (appContext() as MainApplication).appSettingsManager
    val appSettings = appSettingsManager.activeSettings.value
    val isLoginEnabled = appSettings.enableYtLogin
    return isLoginEnabled
}

fun isYtSyncEnabled(): Boolean {
    val appSettingsManager = (appContext() as MainApplication).appSettingsManager
    val appSettings = appSettingsManager.activeSettings.value
    val isSyncEnabled = appSettings.enableYtSync
    return isSyncEnabled && isYtLoggedIn() && isYtLoginEnabled()
}

fun isYtLoggedIn(): Boolean {
    val appSettingsManager = (appContext() as MainApplication).appSettingsManager
    val appSettings = appSettingsManager.activeSettings.value
    val cookie = appSettings.ytCookie
    Timber.d(" AccountSettings isYtLoggedIn cookie = $cookie")
    val isLoggedIn = cookie.let { parseCookieString(it) }.contains("SAPISID") && isYtLoginEnabled()
    return isLoggedIn
}






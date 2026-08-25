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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
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
import it.fast4x.riplay.enums.MusicIdentifierProvider
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.globalContext
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.ValidationType
import it.fast4x.riplay.extensions.discord.DiscordLoginAndGetToken
import it.fast4x.riplay.extensions.accountlogin.AccountLogin
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavBrowserState
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavConfig
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.utils.isAtLeastAndroid81
import it.fast4x.riplay.ui.components.themed.AccountInfoDialog
import it.fast4x.riplay.extensions.lastfm.LastFmAuthScreen
import it.fast4x.riplay.ui.components.themed.Loader
import it.fast4x.riplay.ui.components.themed.SecondaryTextButton
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.CryptoManager
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
fun AccountsSettings() {
    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val context = LocalContext.current
    val thumbnailRoundness = appearanceSettings.thumbnailRoundness

    var showUserInfoDialog by rememberSaveable { mutableStateOf(false) }
    val isEnabledMusicIdentifier = appSettings.enableMusicIdentifier

    val musicIdentifierProvider = appSettings.musicIdentifierProvider

    val musicIdentifierApi = appSettings.musicIdentifierApi

    val uriHandler = LocalUriHandler.current


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
            onClick = {}
        )

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

        val coroutineScope = rememberCoroutineScope()

        SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.title_youtube_music))

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

            }
        )

        AnimatedVisibility(visible = isYouTubeLoginEnabled) {
            Column(
                modifier = Modifier.padding(start = 12.dp)
            ) {

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

                                        cachedAccounts = emptyList()
                                        loginYouTube = false
                                        val cookieManager = CookieManager.getInstance()
                                        cookieManager.removeAllCookies(null)
                                        cookieManager.flush()
                                        WebStorage.getInstance().deleteAllData()
                                    } else
                                        loginYouTube = true
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
                                    }
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

        SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.title_lastfm))

        SwitchSettingEntry(
            title = stringResource(R.string.enable_lastfm),
            text = "",
            isChecked = isEnabledLastfm,
            onCheckedChange = {
                coroutineScope.launch {
                    val new = appSettingsManager.activeSettings.value.copy(isEnabledLastFM = it)
                    appSettingsManager.updateSettings(new)
                }
            },
        )

        AnimatedVisibility(visible = isEnabledLastfm) {
            Column(
                modifier = Modifier.padding(start = 12.dp)
            ) {
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
                    valueText = { it.textName },
                )

            }
        }

        /****** LASTFM ******/

        /****** DISCORD ******/
        val isDiscordPresenceEnabled = appSettings.isDiscordPresenceEnabled
        var loginDiscord by remember { mutableStateOf(false) }
        var showDiscordUserInfoDialog by remember { mutableStateOf(false) }

        val discordPersonalAccessToken = appSettings.discordPersonalAccessToken

        val discordAccountName = appSettings.discordAccountName

        SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.social_discord))
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
            }
        )

        AnimatedVisibility(visible = isDiscordPresenceEnabled) {
            Column(
                modifier = Modifier.padding(start = 12.dp)
            ) {
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


        /****** DISCORD ******/

        /****** WEBDAV ******/
        var webDavSync by remember { mutableStateOf(false) }
        val isWebDavEnabled = appSettings.isWebDavEnabled
        val webDavUrl = appSettings.webDavUrl
        val webDavFolder = appSettings.webDavFolder
        val webDavUsername = appSettings.webDavUsername
        val webDavPassword by remember(appSettings.webDavPassword) { mutableStateOf(CryptoManager.decrypt(appSettings.webDavPassword)) }
        val isWebDavScanSubfoldersEnabled = appSettings.isWebDavScanSubfoldersEnabled
        val webdavViewModel = LocalWebDavLibrary.current
        val webdavUiState by webdavViewModel.uiState.collectAsStateWithLifecycle()

        SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.webdav))

        LaunchedEffect(webDavSync) {
            if (webDavSync) {
                webdavViewModel.loadFolder(
                    WebDavConfig(
                        baseUrl = appSettings.webDavUrl,
                        username = appSettings.webDavUsername,
                        password = CryptoManager.decrypt(appSettings.webDavPassword),
                    ),
                    appSettings.webDavFolder
                )
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

        SwitchSettingEntry(
            title = stringResource(R.string.webdav_enable),
            text = "Personal Cloud (WebDAV)",
            isChecked = isWebDavEnabled,
            onCheckedChange = {
                coroutineScope.launch {
                    val new = appSettingsManager.activeSettings.value.copy(isWebDavEnabled = it)
                    appSettingsManager.updateSettings(new)
                }
            }
        )

        AnimatedVisibility(visible = isWebDavEnabled) {
            Column(
                modifier = Modifier.padding(start = 12.dp)
            ) {
                TextDialogSettingEntry(
                    title = stringResource(R.string.webdav_url),
                    text = webDavUrl,
                    currentText = webDavUrl,
                    onTextSave = {
                        coroutineScope.launch {
                            val new = appSettingsManager.activeSettings.value.copy(webDavUrl = it)
                            appSettingsManager.updateSettings(new)
                        }
                    },
                    validationType = ValidationType.Url
                )
                TextDialogSettingEntry(
                    title = stringResource(R.string.webdav_folder),
                    text = webDavFolder,
                    currentText = webDavFolder,
                    onTextSave = {
                        coroutineScope.launch {
                            val new = appSettingsManager.activeSettings.value.copy(webDavFolder = it)
                            appSettingsManager.updateSettings(new)
                        }
                    }
                )
                SwitchSettingEntry(
                    title = stringResource(R.string.webdav_scan_subfolders),
                    text = "",
                    isChecked = isWebDavScanSubfoldersEnabled,
                    onCheckedChange = {
                        coroutineScope.launch {
                            val new = appSettingsManager.activeSettings.value.copy(isWebDavScanSubfoldersEnabled = it)
                            appSettingsManager.updateSettings(new)
                        }
                    }
                )
                TextDialogSettingEntry(
                    title = stringResource(R.string.webdav_username),
                    text = webDavUsername,
                    currentText = webDavUsername,
                    onTextSave = {
                        coroutineScope.launch {
                            val new = appSettingsManager.activeSettings.value.copy(webDavUsername = it)
                            appSettingsManager.updateSettings(new)
                        }
                    }
                )
                TextDialogSettingEntry(
                    title = stringResource(R.string.webdav_password),
                    text = if (webDavPassword.isNotEmpty()) "********" else "",
                    currentText = webDavPassword,
                    onTextSave = {
                        coroutineScope.launch {
                            // Crittografia della nuova password e salvataggio
                            val cryptedPassword = CryptoManager.encrypt(it)
                            val new = appSettingsManager.activeSettings.value.copy(webDavPassword = cryptedPassword)
                            appSettingsManager.updateSettings(new)
                        }
                    }
                )

                AnimatedVisibility(visible = webDavUrl.isNotEmpty() && webDavUsername.isNotEmpty() && webDavPassword.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingsDescription(
                            text = stringResource(R.string.webdav_sync),
                            important = true,
                            modifier = Modifier.weight(1f)
                        )

                        SecondaryTextButton(
                            text = stringResource(R.string.webdav_sync_now),
                            onClick = { webDavSync = true },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 24.dp)
                        )
                    }
                }

                /*
                AnimatedVisibility(visible = webDavUrl.isNotEmpty() && webDavUsername.isNotEmpty() && webDavPassword.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingsDescription(
                            text = "Database Sync To WebDAV", //stringResource(R.string.webdav_sync),
                            important = true,
                            modifier = Modifier.weight(1f)
                        )

                        SecondaryTextButton(
                            text = stringResource(R.string.webdav_sync_now),
                            onClick = {
                                webdavViewModel.syncDatabaseToWebDav(Dependencies.application)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 24.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = webDavUrl.isNotEmpty() && webDavUsername.isNotEmpty() && webDavPassword.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingsDescription(
                            text = "Database Sync from WebDAV", //stringResource(R.string.webdav_sync),
                            important = true,
                            modifier = Modifier.weight(1f)
                        )

                        SecondaryTextButton(
                            text = stringResource(R.string.webdav_sync_now),
                            onClick = {
                                val appScope = Dependencies.application.appScopeIO
                                appScope.launch {
                                    webdavViewModel.syncDatabaseFromWebDav(
                                        Dependencies.application,
                                        WebDavConfig(
                                            baseUrl = webDavUrl,
                                            username = webDavUsername,
                                            password = webDavPassword
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 24.dp)
                        )
                    }
                }

                 */
            }
        }

        /****** WEBDAV ******/


        /**** MUSIC IDENTIFIER ******/
        SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.title_music_identifier))

        SwitchSettingEntry(
            title = stringResource(R.string.enable_music_identifier),
            text = "",
            isChecked = isEnabledMusicIdentifier,
            onCheckedChange = {
                coroutineScope.launch {
                    val new = appSettingsManager.activeSettings.value.copy(enableMusicIdentifier = it)
                    appSettingsManager.updateSettings(new)
                }
            },
        )

        AnimatedVisibility(visible = isEnabledMusicIdentifier) {
            Column(
                modifier = Modifier.padding(start = 12.dp)
            ) {
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
                    valueText = { it.title },
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
                    Column(
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
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
                            validationType = ValidationType.None,
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






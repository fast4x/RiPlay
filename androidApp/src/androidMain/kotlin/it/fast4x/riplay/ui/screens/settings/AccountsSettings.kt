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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import it.fast4x.environment.Environment
import it.fast4x.environment.models.responses.CachedAccountProfile
import it.fast4x.environment.utils.parseCookieString
import it.fast4x.riplay.LocalAudioTagger
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.LastFmScrobbleType
import it.fast4x.riplay.enums.MusicIdentifierProvider
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.globalContext
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.ValidationType
import it.fast4x.riplay.extensions.discord.DiscordLoginAndGetToken
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISCORD_ACCOUNT_NAME
import it.fast4x.riplay.extensions.accountlogin.AccountLogin
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISCORD_PERSONAL_ACCESS_TOKEN
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ENABLE_YOU_TUBE_LOGIN
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ENABLE_YOU_TUBE_SYNC
import it.fast4x.riplay.utils.isAtLeastAndroid81
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_DISCORD_PRESENCE_ENABLED
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_ROUNDNESS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.USE_YT_LOGIN_ONLY_FOR_BROWSE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.YT_ACCOUNT_CHANNEL_HANDLE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.YT_ACCOUNT_EMAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.YT_ACCOUNT_NAME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.YT_ACCOUNT_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.YT_COOKIE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.YT_DATA_SYNC_ID
import it.fast4x.riplay.extensions.preferences.PreferenceKey.YT_VISITOR_DATA
import it.fast4x.riplay.ui.components.themed.AccountInfoDialog
import it.fast4x.riplay.extensions.encryptedpreferences.rememberEncryptedPreference
import it.fast4x.riplay.extensions.lastfm.LastFmAuthScreen
import it.fast4x.riplay.extensions.preferences.PreferenceKey
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ENABLE_MUSIC_IDENTIFIER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_ENABLED_LASTFM
import it.fast4x.riplay.extensions.preferences.PreferenceKey.LASTFM_SCRUBBLE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.LASTFM_SESSION_TOKEN
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MUSIC_IDENTIFIER_API_KEY
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MUSIC_IDENTIFIER_PROVIDER
import it.fast4x.riplay.ui.components.themed.DefaultDialog
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
fun AccountsSettings() {
    val context = LocalContext.current
    val thumbnailRoundness by rememberPreference(
        THUMBNAIL_ROUNDNESS.key,
        ThumbnailRoundness.Light
    )

    var showUserInfoDialog by rememberSaveable { mutableStateOf(false) }

    var isEnabledMusicIdentifier by rememberPreference(
        ENABLE_MUSIC_IDENTIFIER.key,
        false
    )
    var musicIdentifierProvider by rememberPreference(MUSIC_IDENTIFIER_PROVIDER.key,
        MusicIdentifierProvider.AudioTagInfo)

    var musicIdentifierApi by rememberPreference(MUSIC_IDENTIFIER_API_KEY.key, "")

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

        var useYtLoginOnlyForBrowse by rememberPreference(USE_YT_LOGIN_ONLY_FOR_BROWSE.key, true)
        var isYouTubeLoginEnabled by rememberPreference(ENABLE_YOU_TUBE_LOGIN.key, false)
        var isSyncEnabled by rememberPreference(ENABLE_YOU_TUBE_SYNC.key, true)
        var loginYouTube by remember { mutableStateOf(false) }
        var visitorData by rememberPreference(key = YT_VISITOR_DATA.key, defaultValue = "")
        var dataSyncId by rememberPreference(key = YT_DATA_SYNC_ID.key, defaultValue = "")
        var cookie by rememberPreference(key = YT_COOKIE.key, defaultValue = "")
        var accountName by rememberPreference(key = YT_ACCOUNT_NAME.key, defaultValue = "")
        var accountEmail by rememberPreference(key = YT_ACCOUNT_EMAIL.key, defaultValue = "")
        var accountChannelHandle by rememberPreference(
            key = YT_ACCOUNT_CHANNEL_HANDLE.key,
            defaultValue = ""
        )
        var accountThumbnail by rememberPreference(key = YT_ACCOUNT_THUMBNAIL.key, defaultValue = "")
        var isLoggedIn = remember(cookie) {
            "SID" in parseCookieString(cookie) ||
                    "LOGIN_INFO" in parseCookieString(cookie)
        }

        var jsonCachedAccounts by rememberPreference(PreferenceKey.YT_CACHED_ACCOUNTS.key, "")
        var cachedAccounts = try {
            Json.decodeFromString<List<CachedAccountProfile>>(jsonCachedAccounts)
        } catch (e: Exception) {
            Timber.e(e, "Errore nel parsing della cache account")
            emptyList()
        }



        SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.title_youtube_music))

        SwitchSettingEntry(
            title = stringResource(R.string.enable_youtube_music_login),
            text = "",
            isChecked = isYouTubeLoginEnabled,
            onCheckedChange = {
                isYouTubeLoginEnabled = it
                if (!it) {
                    accountName = ""
                    accountChannelHandle = ""
                    accountEmail = ""
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

                        if (isLoggedIn && accountThumbnail != "")
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
                                title = if (isLoggedIn) stringResource(R.string.disconnect) else stringResource(
                                    R.string.connect
                                ),
                                text = stringResource(R.string.login_connect_or_disconnect_your_account),
                                icon = R.drawable.internet,
                                iconColor = colorPalette().text,
                                onClick = {
                                    if (isLoggedIn) {
                                        cookie = ""
                                        accountName = ""
                                        accountChannelHandle = ""
                                        accountEmail = ""
                                        accountThumbnail = ""
                                        jsonCachedAccounts = ""
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

                            if (isLoggedIn) {

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
                                                    accountName = it?.name.orEmpty()
                                                    accountEmail = it?.email.orEmpty()
                                                    accountChannelHandle =
                                                        it?.channelHandle.orEmpty()
                                                    accountThumbnail = it?.thumbnailUrl.orEmpty()
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
                                        isSyncEnabled = it
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
        var isEnabledLastfm by rememberPreference(IS_ENABLED_LASTFM.key, false)
        var lastFmSessionToken by rememberPreference(LASTFM_SESSION_TOKEN.key, "")
        var loginLastfm by remember { mutableStateOf(false) }
        var lastfmScrobbleType by rememberPreference(
            LASTFM_SCRUBBLE_TYPE.key,
            LastFmScrobbleType.Simple
        )

        SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.title_lastfm))

        SwitchSettingEntry(
            title = stringResource(R.string.enable_lastfm),
            text = "",
            isChecked = isEnabledLastfm,
            onCheckedChange = {
                isEnabledLastfm = it
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
                        if (lastFmSessionToken.isNotEmpty())
                            lastFmSessionToken = ""
                        else
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
                            lastFmSessionToken =
                                context.preferences.getString(LASTFM_SESSION_TOKEN.key, "") ?: ""
                            Timber.d("LastFmAuthScreen: Authentication complete")
                        }
                    )
                }

                EnumValueSelectorSettingsEntry(
                    title = stringResource(R.string.lastfm_scrobble_type),
                    titleSecondary = "",
                    selectedValue = lastfmScrobbleType,
                    onValueSelected = { lastfmScrobbleType = it },
                    valueText = { it.textName },
                )

            }
        }

        /****** LASTFM ******/

        /****** DISCORD ******/
        var isDiscordPresenceEnabled by rememberPreference(IS_DISCORD_PRESENCE_ENABLED.key, false)
        var loginDiscord by remember { mutableStateOf(false) }
        var showDiscordUserInfoDialog by remember { mutableStateOf(false) }
        var discordPersonalAccessToken by rememberEncryptedPreference(
            key = DISCORD_PERSONAL_ACCESS_TOKEN.key,
            defaultValue = ""
        )
        var discordAccountName by rememberEncryptedPreference(
            key = DISCORD_ACCOUNT_NAME.key,
            defaultValue = ""
        )
        SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.social_discord))
        SwitchSettingEntry(
            isEnabled = isAtLeastAndroid81,
            title = stringResource(R.string.discord_enable_rich_presence),
            text = "",
            isChecked = isDiscordPresenceEnabled,
            onCheckedChange = { isDiscordPresenceEnabled = it }
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
                        if (discordPersonalAccessToken.isNotEmpty())
                            discordPersonalAccessToken = ""
                        else
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
                            discordPersonalAccessToken = token
                            discordAccountName = username
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

        /**** MUSIC IDENTIFIER ******/
        SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.title_music_identifier))

        SwitchSettingEntry(
            title = stringResource(R.string.enable_music_identifier),
            text = "",
            isChecked = isEnabledMusicIdentifier,
            onCheckedChange = {
                isEnabledMusicIdentifier = it
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
                    onValueSelected = { musicIdentifierProvider = it },
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
                                musicIdentifierApi = it
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
    val isLoginEnabled = appContext().preferences.getBoolean(ENABLE_YOU_TUBE_LOGIN.key, false)
    return isLoginEnabled
}

fun isYtSyncEnabled(): Boolean {
    val isSyncEnabled = appContext().preferences.getBoolean(ENABLE_YOU_TUBE_SYNC.key, true)
    return isSyncEnabled && isYtLoggedIn() && isYtLoginEnabled()
}

fun isYtLoggedIn(): Boolean {
    val cookie = appContext().preferences.getString(YT_COOKIE.key, "")
    val isLoggedIn = cookie?.let { parseCookieString(it) }?.contains("SAPISID") == true && isYtLoginEnabled()
    return isLoggedIn
}






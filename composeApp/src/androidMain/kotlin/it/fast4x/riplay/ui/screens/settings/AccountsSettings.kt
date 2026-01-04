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
import androidx.compose.material3.rememberModalBottomSheetState
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
import it.fast4x.environment.utils.parseCookieString
import it.fast4x.riplay.LocalAudioTagger
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.MusicIdentifierProvider
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.globalContext
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.ValidationType
import it.fast4x.riplay.extensions.discord.DiscordLoginAndGetToken
import it.fast4x.riplay.extensions.preferences.discordAccountNameKey
import it.fast4x.riplay.extensions.youtubelogin.YouTubeLogin
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.extensions.preferences.discordPersonalAccessTokenKey
import it.fast4x.riplay.extensions.preferences.enableYouTubeLoginKey
import it.fast4x.riplay.extensions.preferences.enableYouTubeSyncKey
import it.fast4x.riplay.utils.isAtLeastAndroid81
import it.fast4x.riplay.extensions.preferences.isDiscordPresenceEnabledKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.extensions.preferences.useYtLoginOnlyForBrowseKey
import it.fast4x.riplay.extensions.preferences.ytAccountChannelHandleKey
import it.fast4x.riplay.extensions.preferences.ytAccountEmailKey
import it.fast4x.riplay.extensions.preferences.ytAccountNameKey
import it.fast4x.riplay.extensions.preferences.ytAccountThumbnailKey
import it.fast4x.riplay.extensions.preferences.ytCookieKey
import it.fast4x.riplay.extensions.preferences.ytDataSyncIdKey
import it.fast4x.riplay.extensions.preferences.ytVisitorDataKey
import it.fast4x.riplay.ui.components.themed.AccountInfoDialog
import it.fast4x.riplay.extensions.encryptedpreferences.rememberEncryptedPreference
import it.fast4x.riplay.extensions.lastfm.LastFmAuthScreen
import it.fast4x.riplay.extensions.preferences.enableMusicIdentifierKey
import it.fast4x.riplay.extensions.preferences.isEnabledLastfmKey
import it.fast4x.riplay.extensions.preferences.lastfmSessionTokenKey
import it.fast4x.riplay.extensions.preferences.musicIdentifierApiKey
import it.fast4x.riplay.extensions.preferences.musicIdentifierProviderKey
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    var showUserInfoDialog by rememberSaveable { mutableStateOf(false) }

    var isEnabledMusicIdentifier by rememberPreference(
        enableMusicIdentifierKey,
        true
    )
    var musicIdentifierProvider by rememberPreference(musicIdentifierProviderKey,
        MusicIdentifierProvider.AudioTagInfo)

    var musicIdentifierApi by rememberPreference(musicIdentifierApiKey, "")

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

        var useYtLoginOnlyForBrowse by rememberPreference(useYtLoginOnlyForBrowseKey, true)
        var isYouTubeLoginEnabled by rememberPreference(enableYouTubeLoginKey, false)
        var isYouTubeSyncEnabled by rememberPreference(enableYouTubeSyncKey, false)
        var loginYouTube by remember { mutableStateOf(false) }
        var visitorData by rememberPreference(key = ytVisitorDataKey, defaultValue = "")
        var dataSyncId by rememberPreference(key = ytDataSyncIdKey, defaultValue = "")
        var cookie by rememberPreference(key = ytCookieKey, defaultValue = "")
        var accountName by rememberPreference(key = ytAccountNameKey, defaultValue = "")
        var accountEmail by rememberPreference(key = ytAccountEmailKey, defaultValue = "")
        var accountChannelHandle by rememberPreference(
            key = ytAccountChannelHandleKey,
            defaultValue = ""
        )
        var accountThumbnail by rememberPreference(key = ytAccountThumbnailKey, defaultValue = "")
        var isLoggedIn = remember(cookie) {
            "SAPISID" in parseCookieString(cookie)
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
                                text = "",
                                icon = R.drawable.internet,
                                iconColor = colorPalette().text,
                                onClick = {
                                    if (isLoggedIn) {
                                        cookie = ""
                                        accountName = ""
                                        accountChannelHandle = ""
                                        accountEmail = ""
                                        accountThumbnail = ""
                                        loginYouTube = false
                                        val cookieManager = CookieManager.getInstance()
                                        cookieManager.removeAllCookies(null)
                                        cookieManager.flush()
                                        WebStorage.getInstance().deleteAllData()
                                    } else
                                        loginYouTube = true
                                }
                            )

                            ButtonBarSettingEntry(
                                isEnabled = true,
                                title = stringResource(R.string.account_info),
                                text = "",
                                icon = R.drawable.person,
                                iconColor = colorPalette().text,
                                onClick = {
                                    if (accountThumbnail == "" || accountName == "" || accountEmail == "")
                                        GlobalScope.launch {
                                            Environment.accountInfo().onSuccess {
                                                println("YoutubeLogin doUpdateVisitedHistory accountInfo() $it")
                                                accountName = it?.name.orEmpty()
                                                accountEmail = it?.email.orEmpty()
                                                accountChannelHandle = it?.channelHandle.orEmpty()
                                                accountThumbnail = it?.thumbnailUrl.orEmpty()
                                            }.onFailure {
                                                Timber.e("Error YoutubeLogin: $it.stackTraceToString()")
                                                println("Error YoutubeLogin: ${it.stackTraceToString()}")
                                            }
                                        }
                                    showUserInfoDialog = true
                                }
                            )


                            CustomModalBottomSheet(
                                showSheet = loginYouTube,
                                onDismissRequest = {
                                    loginYouTube = false
                                },
                                containerColor = colorPalette().background0,
                                contentColor = colorPalette().background0,
                                modifier = Modifier.fillMaxWidth(),
                                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                                dragHandle = {
                                    Surface(
                                        modifier = Modifier.padding(vertical = 0.dp),
                                        color = colorPalette().background0,
                                        shape = thumbnailShape()
                                    ) {}
                                },
                                shape = thumbnailRoundness.shape()
                            ) {
                                YouTubeLogin(
                                    onLogin = { cookieRetrieved ->
                                        cookie = cookieRetrieved
                                        if (cookieRetrieved.contains("SAPISID")) {
                                            isLoggedIn = true
                                            loginYouTube = false
                                            SmartMessage(
                                                "Login successful",
                                                type = PopupType.Info,
                                                context = context
                                            )

                                        }

                                    }
                                )
                            }

                        }

                    }

                SwitchSettingEntry(
                    title = stringResource(R.string.sync_data_with_ytm_account),
                    text = stringResource(R.string.sync_data_playlists_albums_artists_history_like_etc),
                    isChecked = isYouTubeSyncEnabled,
                    onCheckedChange = {
                        isYouTubeSyncEnabled = it
                    }
                )

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

    /****** DISCORD ******/

        var isDiscordPresenceEnabled by rememberPreference(isDiscordPresenceEnabledKey, false)
        var loginDiscord by remember { mutableStateOf(false) }
        var showDiscordUserInfoDialog by remember { mutableStateOf(false) }
        var discordPersonalAccessToken by rememberEncryptedPreference(
            key = discordPersonalAccessTokenKey,
            defaultValue = ""
        )
        var discordAccountName by rememberEncryptedPreference(
            key = discordAccountNameKey,
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
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                            loginDiscord = false
                            discordPersonalAccessToken = token
                            discordAccountName = username
                            SmartMessage(globalContext().resources.getString(R.string.discord_connected_to_discord_account) + " $username", type = PopupType.Info, context = context)
                        }
                    )
                }
            }
        }

    /****** DISCORD ******/

    var isEnabledLastfm by rememberPreference(isEnabledLastfmKey, false)
        var lastFmSessionToken by rememberPreference(lastfmSessionTokenKey, "")
        var loginLastfm by remember { mutableStateOf(false) }



        SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.title_lastfm))

        SwitchSettingEntry(
            title = stringResource(R.string.enable_lastfm),
            text = "",
            isChecked = isEnabledLastfm,
            onCheckedChange = {
                isEnabledLastfm = it
            },
            offline = false,
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
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                            lastFmSessionToken = context.preferences.getString(lastfmSessionTokenKey, "") ?: ""
                            Timber.d("LastFmAuthScreen: Authentication complete")
                        }
                    )
                }

            }
        }

        SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.title_music_identifier))

        SwitchSettingEntry(
            title = stringResource(R.string.enable_music_identifier),
            text = "",
            isChecked = isEnabledMusicIdentifier,
            onCheckedChange = {
                isEnabledMusicIdentifier = it
            },
            offline = false
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
                    offline = false
                )
                SettingsEntry(
                    online = false,
                    offline = false,
                    title = musicIdentifierProvider.subtitle,
                    text = musicIdentifierProvider.website,
                    onClick = {
                        uriHandler.openUri(musicIdentifierProvider.website)
                    }
                )


                AnimatedVisibility(visible = musicIdentifierProvider == MusicIdentifierProvider.AudioTagInfo) {
                    Column (
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
                            offline = false,
                            online = false
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

        //Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
    }



}

fun isYtLoginEnabled(): Boolean {
    val isLoginEnabled = appContext().preferences.getBoolean(enableYouTubeLoginKey, false)
    return isLoginEnabled
}

fun isYtSyncEnabled(): Boolean {
    val isSyncEnabled = appContext().preferences.getBoolean(enableYouTubeSyncKey, false)
    return isSyncEnabled && isYtLoggedIn() && isYtLoginEnabled()
}

fun isYtLoggedIn(): Boolean {
    val cookie = appContext().preferences.getString(ytCookieKey, "")
    val isLoggedIn = cookie?.let { parseCookieString(it) }?.contains("SAPISID") == true && isYtLoginEnabled()
    return isLoggedIn
}






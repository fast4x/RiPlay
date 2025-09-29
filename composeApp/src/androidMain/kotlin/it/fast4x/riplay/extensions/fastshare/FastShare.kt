package it.fast4x.riplay.extensions.fastshare

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import it.fast4x.riplay.Database
import it.fast4x.riplay.R
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.context
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.ShareType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.LinkType
import it.fast4x.riplay.extensions.listapps.listApps
import it.fast4x.riplay.extensions.listapps.toExternalApp
import it.fast4x.riplay.models.Album
import it.fast4x.riplay.models.Artist
import it.fast4x.riplay.models.ExternalApp
import it.fast4x.riplay.models.Playlist
import it.fast4x.riplay.thumbnailShape
import it.fast4x.riplay.typography
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.components.themed.DefaultDialog
import it.fast4x.riplay.ui.components.themed.MenuEntry
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.TitleMiniSection
import it.fast4x.riplay.ui.components.themed.TitleSection
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.extensions.preferences.rememberObservedPreference
import it.fast4x.riplay.utils.copyTextToClipboard
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import kotlinx.coroutines.Dispatchers

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FastShare(
    showFastShare: Boolean,
    showLinks: Boolean? = true,
    showShareWith: Boolean? = true,
    showShareWithExternalApps: Boolean? = true,
    //typeOfShare: ShareType = ShareType.Classic,
    //typeOfUrl: LinkType = LinkType.Alternative,
    onDismissRequest: () -> Unit,
    content: Any,
) {
    //if (content.toString().isEmpty()) return

//    if (content.toString().isEmpty()) {
//        SmartMessage(message = "No content to share!", type = PopupType.Error, context = context())
//        return
//    }

    var typeOfUrl by remember { mutableStateOf(LinkType.Main) }
    var urlToShare by remember { mutableStateOf("") }

    LaunchedEffect(Unit, typeOfUrl) {
        urlToShare = when (typeOfUrl) {
            LinkType.Main -> {
                when (content) {
                    is MediaItem -> content.asSong.shareYTUrl
                    is Playlist -> {
                        if (!content.isPodcast) content.shareYTUrl
                        else content.shareYTUrlAsPodcast
                    }

                    is Album -> content.shareYTUrl
                    is Artist -> content.shareYTUrl
                    else -> ""
                }
            }

            LinkType.Alternative -> {
                when (content) {
                    is MediaItem -> content.asSong.shareYTMUrl
                    is Playlist -> {
                        if (!content.isPodcast) content.shareYTMUrl
                        else content.shareYTMUrlAsPodcast
                    }

                    is Album -> content.shareYTMUrl
                    is Artist -> content.shareYTMUrl
                    else -> ""
                }
            }

        }.toString()
    }

    if (urlToShare == "") return


    val thumbnailRoundness by rememberObservedPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    var showAppSelector by remember { mutableStateOf(false) }

    val externalApps by remember {
        Database.externalApps()
    }.collectAsState(initial = emptyList() , Dispatchers.IO)
    
    val uriHandler = LocalUriHandler.current

    CustomModalBottomSheet(
        showSheet = showFastShare,
        onDismissRequest = onDismissRequest,
        containerColor = colorPalette().background0,
        contentColor = colorPalette().background0,
        modifier = Modifier
            .fillMaxWidth(),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 0.dp),
                color = colorPalette().background0,
                shape = thumbnailShape()
            ) {
                Image(
                    painter = painterResource(R.drawable.share_social),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette().text),
                    modifier = Modifier
                        .size(30.dp)
                )
            }
        },
        shape = thumbnailRoundness.shape()
    ) {
        Column(
            modifier = Modifier
                .padding(all = 20.dp)
                .background(
                    colorPalette().background0
                )
                .fillMaxWidth()
        ) {

            if (showLinks == true) {
                Row(
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(1.dp, colorPalette().accent),
                                shape = thumbnailShape()
                            )
                    ) {
                        TitleMiniSection(
                            title = "Copy link"
                        )
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Use main link",
                                fontSize = typography().xs.fontSize,
                                fontFamily = typography().xs.fontFamily,
                                fontWeight = typography().xs.fontWeight,
                                fontStyle = typography().xs.fontStyle,
                                color = colorPalette().text,
                            )
                            Checkbox(
                                checked = typeOfUrl == LinkType.Main,
                                onCheckedChange = {
                                    if (it) typeOfUrl = LinkType.Main else typeOfUrl =
                                        LinkType.Alternative
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colorPalette().accent,
                                    uncheckedColor = colorPalette().text
                                ),
                                modifier = Modifier
                                    .scale(0.7f),
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .requiredHeight(60.dp)
                                .background(colorPalette().background2, shape = thumbnailShape())
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = urlToShare,
                                fontSize = typography().xs.fontSize,
                                fontFamily = typography().xs.fontFamily,
                                fontWeight = typography().xs.fontWeight,
                                fontStyle = typography().xs.fontStyle,
                                color = colorPalette().text,
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .clickable {
                                        copyTextToClipboard(urlToShare, context())
                                    }
                            )
                            Image(
                                painter = painterResource(R.drawable.copy),
                                colorFilter = ColorFilter.tint(colorPalette().text),
                                contentDescription = "Copy link",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        copyTextToClipboard(urlToShare, context())
                                    }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(1.dp, colorPalette().accent),
                                shape = thumbnailShape()
                            )
                    ) {
                        TitleMiniSection(
                            title = "Open link",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .requiredHeight(60.dp)
                                .background(colorPalette().background2, shape = thumbnailShape())
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = urlToShare,
                                fontSize = typography().xs.fontSize,
                                fontFamily = typography().xs.fontFamily,
                                fontWeight = typography().xs.fontWeight,
                                fontStyle = typography().xs.fontStyle,
                                color = colorPalette().text,
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .clickable {
                                        uriHandler.openUri(urlToShare)
                                    }
                            )
                            Image(
                                painter = painterResource(R.drawable.internet),
                                colorFilter = ColorFilter.tint(colorPalette().text),
                                contentDescription = "Open link",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        uriHandler.openUri(urlToShare)
                                    }
                            )
                        }

                    }
                }
            }
            if (showShareWith == true) {
                Row(
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(1.dp, colorPalette().accent),
                                shape = thumbnailShape()
                            )
                    ) {
                        TitleMiniSection(
                            title = "Share with...",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .requiredHeight(60.dp)
                                .background(colorPalette().background2, shape = thumbnailShape())
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Choose destination...",
                                fontSize = typography().xs.fontSize,
                                fontFamily = typography().xs.fontFamily,
                                fontWeight = typography().xs.fontWeight,
                                fontStyle = typography().xs.fontStyle,
                                color = colorPalette().text,
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .clickable {
                                        classicShare(urlToShare, context())
                                    }
                            )
                            Image(
                                painter = painterResource(R.drawable.share_social),
                                colorFilter = ColorFilter.tint(colorPalette().text),
                                contentDescription = "Share with...",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        classicShare(urlToShare, context())
                                    }
                            )
                        }

                    }
                }
            }

            if (showShareWithExternalApps == true) {
                Row(
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier
                            .height(300.dp)
                            .fillMaxWidth()
                            .border(
                                BorderStroke(1.dp, colorPalette().accent),
                                shape = thumbnailShape()
                            )
                    ) {
                        stickyHeader {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colorPalette().background0)
                            ) {
                                TitleMiniSection(
                                    title = stringResource(R.string.share_with_external_app),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .requiredHeight(60.dp)
                                    .background(
                                        colorPalette().background2,
                                        shape = thumbnailShape()
                                    )
                                    .padding(horizontal = 20.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = "Choose installed app",
                                    fontSize = typography().xs.fontSize,
                                    fontFamily = typography().xs.fontFamily,
                                    fontWeight = typography().xs.fontWeight,
                                    fontStyle = typography().xs.fontStyle,
                                    color = colorPalette().text,
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .clickable {
                                            showAppSelector = true
                                        }
                                )
                                Image(
                                    painter = painterResource(R.drawable.add_app),
                                    colorFilter = ColorFilter.tint(colorPalette().text),
                                    contentDescription = "Add app",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable {
                                            showAppSelector = true
                                        }
                                )
                            }
                        }
                        items(externalApps.size) {
                            val app = externalApps[it]
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .border(
                                        BorderStroke(1.dp, colorPalette().background2),
                                        shape = thumbnailShape()
                                    )
                                    //.background(colorPalette().background2, shape = thumbnailShape())
                                    .fillMaxWidth()
                                    .padding(all = 10.dp)
                                    .clickable {
                                        directShare(urlToShare, app.componentName, context())
                                    }

                            ) {
                                Text(
                                    text = app.appName.toString(),
                                    color = colorPalette().text,
                                    fontSize = typography().xs.fontSize,
                                    fontFamily = typography().xs.fontFamily,
                                    fontWeight = typography().xs.fontWeight,
                                    fontStyle = typography().xs.fontStyle,
                                    modifier = Modifier
                                )
                                Image(
                                    painter = painterResource(R.drawable.close),
                                    colorFilter = ColorFilter.tint(colorPalette().text),
                                    contentDescription = "Delete app",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable {
                                            Database.asyncTransaction {
                                                delete(app)
                                            }
                                        }
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                        }

                    }
                }
            }
        }

    }

    if (showAppSelector)
        AppSelector(
            onSelected = {
                showAppSelector = false
                Database.asyncTransaction {
                    insert(it)
                }
                //directShare(urlToShare, it.componentName, context())
            },
            onDismiss = { showAppSelector = false }
        )

}

fun directShare(content: String, componentName: ComponentName, context: Context) {
    if (content.isEmpty() || componentName.packageName.isEmpty()) {
        SmartMessage(message = "No content to share!", type = PopupType.Error, context = context)
        return
    }

    try {
        val intent = Intent().apply {
//            component = ComponentName(
//                "com.junkfood.seal",
//                "com.junkfood.seal.MainActivity"
//            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            component = componentName
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                content
            )
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        SmartMessage(
            "App is not installed! \n${e.localizedMessage}",
            PopupType.Error,
            context = context
        )
    }
}

fun classicShare(content: String, context: Context) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            content
        )
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(shareIntent)
}

@Composable
fun AppSelector(
    onSelected: (ExternalApp) -> Unit,
    onDismiss: () -> Unit
) {
    val apps = remember { listApps(context()).sortedBy { it.appName } }
    DefaultDialog(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TitleSection(title = "Installed applications")
        }
        apps.forEach { app ->
            MenuEntry(
                icon = R.drawable.add_app,
                text = app.appName,
                secondaryText = app.packageName,
                onClick = {
                    onSelected(
                        app.toExternalApp()
                    )
                }
            )
        }
    }
}


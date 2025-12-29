package it.fast4x.riplay.extensions.blacklist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.DefaultDialog
import it.fast4x.riplay.ui.components.themed.DialogTextButton
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.styling.center
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun BlacklistScreen(navController: NavHostController) {
    var showStringAddDialog by remember {
        mutableStateOf(false)
    }
    var showStringRemoveDialog by remember {
        mutableStateOf(false)
    }
    var removingItem by remember { mutableStateOf("") }
    var errorDialog by remember { mutableStateOf(false) }

    val list = remember {
        Database.blacklists()
    }.collectAsState(initial = null, context = Dispatchers.IO)

    var currentBlacklist: Blacklist? by remember { mutableStateOf(null) }


    //Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(color = colorPalette().background1, shape = RoundedCornerShape(8.dp))
                .padding(WindowInsets.systemBars.asPaddingValues())
                .fillMaxWidth()
                .fillMaxHeight(.7f)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 24.dp),
            ) {
                BasicText(
                    text = "Blacklist",
                    style = typography().m.semiBold,
                    modifier = Modifier
                    //.padding(vertical = 8.dp, horizontal = 24.dp)
                )
                Image(
                    painter = painterResource(R.drawable.add),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette().text),
                    modifier = Modifier
                        .clickable { showStringAddDialog = true }
                )

            }

            Spacer(modifier = Modifier.height(5.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                list.value?.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    ) {
                        Image(
                            painter = painterResource(when(item.type) {
                                BlacklistType.Folder.name -> R.drawable.folder
                                BlacklistType.Artist.name -> R.drawable.artist
                                BlacklistType.Album.name -> R.drawable.album
                                BlacklistType.Song.name -> R.drawable.musical_note
                                BlacklistType.Playlist.name -> R.drawable.music_library
                                else -> R.drawable.text
                            }),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(if (item.isEnabled) colorPalette().text else colorPalette().textDisabled),
                            modifier = Modifier
                                .size(24.dp)
                        )
                        BasicText(
                            text = item.path,
                            style = typography().s.semiBold.copy(color = if (item.isEnabled) colorPalette().text else colorPalette().textDisabled),
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                        )
                        Image(
                            painter = painterResource(if (item.isEnabled) R.drawable.eye else R.drawable.eye_off),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(if (item.isEnabled) colorPalette().text else colorPalette().textDisabled),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        Database.update(item.toggleEnabled())
                                    }
                                }
                        )
                        Image(
                            painter = painterResource(R.drawable.trash),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette().red),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    currentBlacklist = item
                                    showStringRemoveDialog = true
                                }
                        )

                    }
                }
            }

        }

    //}

    if (showStringAddDialog) {
        InputTextDialog(
            onDismiss = { showStringAddDialog = false },
            placeholder = "Placeholder",
            setValue = {
//                if (it !in list) {
//                    add(it)
//                } else {
//                    errorDialog = true
//                }
            },
            title = "Add",
            value = ""
        )
    }

    if (showStringRemoveDialog) {
        ConfirmationDialog(
            text = "Remove",
            onDismiss = { showStringRemoveDialog = false },
            onConfirm = {
                CoroutineScope(Dispatchers.IO).launch {
                    currentBlacklist?.let {
                        Database.delete(it)
                    }
                }
            }
        )
    }

    if (errorDialog) {
        DefaultDialog(
            onDismiss = {errorDialog = false},
            modifier = Modifier
        ) {
            BasicText(
                text = "Conflict",
                style = typography().xs.medium.center,
                modifier = Modifier
                    .padding(all = 16.dp)
            )

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
            ) {

                DialogTextButton(
                    text = stringResource(R.string.confirm),
                    primary = true,
                    onClick = {
                        errorDialog = false
                    }
                )
            }
        }
    }
}
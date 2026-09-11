package it.fast4x.riplay.extensions.webdavlibrary.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.data.models.WebDavAccount
import it.fast4x.riplay.data.models.webDavAccountEmpty
import it.fast4x.riplay.extensions.webdavlibrary.WebDavLibraryViewModel
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.utils.CryptoManager
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.R
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.ui.styling.semiBold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavAccountFormSheet(
    showSheet: Boolean = true,
    isEditing: Boolean,
    account: WebDavAccount = webDavAccountEmpty(),
    testState: WebDavLibraryViewModel.TestConnectionState = WebDavLibraryViewModel.TestConnectionState.Idle,
    onTestConnection: (account: WebDavAccount) -> Unit,
    onSave: (account: WebDavAccount) -> Unit,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberScrollState()

    // Stati dei campi di testo
    var name by remember(account) { mutableStateOf(account.name) }
    var url by remember(account) { mutableStateOf(account.baseUrl) }
    var user by remember(account) { mutableStateOf(account.username) }
    var pass by remember(account) { mutableStateOf(if (isEditing)
        CryptoManager.decrypt(account.encryptedPassword)
    else account.encryptedPassword )
    }
    var folder by remember(account) { mutableStateOf(account.remoteFolder) }
    var isMusicSource by remember(account) { mutableStateOf(account.isMusicSource) }
    var isWebDavScanSubfoldersEnabled by remember(account) { mutableStateOf(account.scanSubFolders) }

    var passwordVisible by remember { mutableStateOf(false) }

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = colorPalette().background1,
        unfocusedContainerColor = colorPalette().background1,
        focusedTextColor = colorPalette().text,
        unfocusedTextColor = colorPalette().textSecondary,
        focusedIndicatorColor = colorPalette().text,
        unfocusedIndicatorColor = colorPalette().textSecondary,
        focusedLabelColor = colorPalette().text,
        unfocusedLabelColor = colorPalette().textSecondary,
        focusedLeadingIconColor = colorPalette().text,
        unfocusedLeadingIconColor = colorPalette().textSecondary,
        focusedTrailingIconColor = colorPalette().text,
        unfocusedTrailingIconColor = colorPalette().textSecondary,
        cursorColor = colorPalette().text,
        focusedPlaceholderColor = colorPalette().textSecondary,
        unfocusedPlaceholderColor = colorPalette().textSecondary
    )

    CustomModalBottomSheet(
        showSheet = showSheet,
        onDismissRequest = onDismiss,
        containerColor = colorPalette().background1,
        contentColor = colorPalette().text,
    ) {
        Column(
            modifier = Modifier
                .background(colorPalette().background1)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(if (isEditing) R.string.webdav_form_title_edit else R.string.webdav_form_title_add),
                style = typography().xl,
                modifier = Modifier.padding(top = 16.dp)
            )
            if (!isMusicSource)
                Text(
                    text = stringResource( R.string.settings_webdav_backup_subtitle),
                    style = typography().xxs.semiBold.copy(color = colorPalette().accent),
                    modifier = Modifier.padding(top = 4.dp)
                )
            else
                Text(
                    text = stringResource( R.string.settings_webdav_link_music_subtitle),
                    style = typography().xxs.semiBold.copy(color = colorPalette().accent),
                    modifier = Modifier.padding(top = 4.dp)
                )

            // Campo NOME
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.webdav_form_name_label)) },
                leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            // Campo URL
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.webdav_form_url_label)) },
                leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            // Campo USERNAME
            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text(stringResource(R.string.webdav_form_username_label)) },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            // Campo PASSWORD
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = {
                    Text(stringResource(if (isEditing) R.string.webdav_form_password_edit_label else R.string.webdav_form_password_add_label))
                },
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = stringResource(if (passwordVisible) R.string.webdav_form_hide_password else R.string.webdav_form_show_password)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            // Campo CARTELLA
            OutlinedTextField(
                value = folder,
                onValueChange = { folder = it },
                label = { Text(stringResource(R.string.webdav_form_folder_label)) },
                leadingIcon = { Icon(Icons.Rounded.Folder, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.webdav_form_folder_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            // SWITCH SORGENTE MUSICALE
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colorPalette().textSecondary.copy(alpha = .2f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colorPalette().text)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.webdav_form_music_source_title), style = typography().m)
                        Text(stringResource(R.string.webdav_form_music_source_subtitle),
                            style = typography().xs,
                            color = colorPalette().textSecondary)
                    }
                    Switch(
                        checked = isMusicSource, onCheckedChange = { isMusicSource = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorPalette().text,
                            checkedTrackColor = colorPalette().textDisabled
                        )
                    )
                }
            }

            AnimatedVisibility(
                visible = isMusicSource,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorPalette().textSecondary.copy(alpha = .2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.FolderSpecial,
                            contentDescription = null,
                            tint = colorPalette().text
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.webdav_form_subfolders_title), style = typography().m)
                            Text(
                                stringResource(R.string.webdav_form_subfolders_subtitle),
                                style = typography().xs,
                                color = colorPalette().textSecondary
                            )
                        }
                        Switch(
                            checked = isWebDavScanSubfoldersEnabled,
                            onCheckedChange = { isWebDavScanSubfoldersEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colorPalette().text,
                                checkedTrackColor = colorPalette().textDisabled
                            )
                        )
                    }
                }
            }


            // SEZIONE TEST CONNESSIONE
            OutlinedButton(
                onClick = {
                    onTestConnection(account.copy(
                        name = name,
                        baseUrl = url,
                        username = user,
                        encryptedPassword = CryptoManager.encrypt(pass),
                        remoteFolder = folder,
                        isMusicSource = isMusicSource,
                        scanSubFolders = isWebDavScanSubfoldersEnabled
                    ))
                },
                enabled = testState !is WebDavLibraryViewModel.TestConnectionState.Testing
                        && testState !is WebDavLibraryViewModel.TestConnectionState.Success,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (testState is WebDavLibraryViewModel.TestConnectionState.Testing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colorPalette().text
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.webdav_form_testing), color = colorPalette().text)
                } else {
                    Icon(Icons.Rounded.WifiFind, contentDescription = null, modifier = Modifier.size(18.dp), tint = colorPalette().text)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.webdav_form_test_button), color = colorPalette().text)
                }
            }

            // Feedback del test (Successo / Errore)
            when (testState) {
                is WebDavLibraryViewModel.TestConnectionState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colorPalette().text)
                        Spacer(Modifier.width(8.dp))
                        Text(testState.message, color = colorPalette().text, style = typography().s)
                    }
                }
                is WebDavLibraryViewModel.TestConnectionState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Error, contentDescription = null, tint = colorPalette().red)
                        Spacer(Modifier.width(8.dp))
                        Text(testState.message, color = colorPalette().red, style = typography().s)
                    }
                }
                else -> {}
            }

            // PULSANTE SALVA
            Button(
                onClick = {
                    onSave(account.copy(
                        name = name.ifEmpty { url },
                        baseUrl = url,
                        username = user,
                        encryptedPassword = CryptoManager.encrypt(pass),
                        remoteFolder = folder,
                        isMusicSource = isMusicSource,
                        scanSubFolders = isWebDavScanSubfoldersEnabled
                    )
                    )
                },
                // Abilitato al Salva solo se il test è ok
                enabled = testState is WebDavLibraryViewModel.TestConnectionState.Success,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.webdav_form_save_button), style = typography().s)
            }
        }
    }
}

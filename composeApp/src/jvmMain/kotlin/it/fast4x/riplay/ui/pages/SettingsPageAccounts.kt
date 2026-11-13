package it.fast4x.riplay.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import it.fast4x.riplay.data.models.WebDavAccount
import it.fast4x.riplay.data.models.webDavAccountEmpty
import it.fast4x.riplay.extensions.listenbrainz.DesktopListenBrainzClient
import it.fast4x.riplay.extensions.lastfm.DesktopLastFmAuthPanel
import it.fast4x.riplay.extensions.discord.DesktopDiscordLoginPanel
import it.fast4x.riplay.extensions.webdav.DesktopWebDavManager
import it.fast4x.riplay.extensions.webdav.WebDavBackupInfo
import it.fast4x.riplay.i18n.ds
import it.fast4x.riplay.ui.components.RiPlayAccent
import it.fast4x.riplay.utils.DesktopAccountsStore
import it.fast4x.riplay.utils.DesktopCryptoManager
import it.fast4x.riplay.utils.DesktopScrobbleType
import kotlinx.coroutines.launch

/**
 * STEP 3ao: SEZIONI ACCOUNT della tab ACCOUNT desktop oltre YouTube Music,
 * replica della pagina AccountsSettings.kt Android:
 *
 *   ┌─ WebDAV ────────────── switch + account (form con test connessione)
 *   │                        + sincronizzazione musica + backup/restore db
 *   ├─ Last.fm ───────────── switch + login (browser integrato) + scrobble
 *   ├─ ListenBrainz ──────── switch + token (con validazione) + server
 *   │                        self-hosted + scrobble
 *   └─ Discord ───────────── switch + login (browser integrato + token
 *                            manuale) + info account
 *
 * Lo stile e' quello della tab (card arrotondate scure, accento RiPlay),
 * con lo stesso significato dei controlli Android (per WebDAV: "Salva"
 * abilitato SOLO dopo un test riuscito, un solo account di backup, ecc.).
 */

// ─────────────────────────────────────────────────────────────────────────────
// Helper di stile condivisi (repliche desktop delle voci Android)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccountSectionTitle(text: String) {
    Text(
        text = text,
        color = RiPlayAccent,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun AccountCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) { content() }
}

@Composable
private fun AccountSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onChecked(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (checked) RiPlayAccent else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (description.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedTrackColor = RiPlayAccent,
                checkedThumbColor = Color(0xFF0B0B10),
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.75f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.18f)
            )
        )
    }
}

@Composable
private fun AccountActionButton(
    label: String,
    emphasized: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = when {
            !enabled -> Color.White.copy(alpha = 0.35f)
            emphasized -> Color.Black
            else -> Color.White
        },
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.06f)
                    emphasized -> RiPlayAccent
                    else -> Color.White.copy(alpha = 0.16f)
                }
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun AccountTextFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isPassword: Boolean = false,
    showValue: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation =
                if (isPassword && !showValue) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 9.dp)
        )
        if (placeholder.isNotBlank() && value.isBlank()) {
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

/** Selettore del tipo di scrobble (chips Simple / NowPlaying, come Android). */
@Composable
private fun ScrobbleTypeChips(
    title: String,
    selected: DesktopScrobbleType,
    onSelect: (DesktopScrobbleType) -> Unit
) {
    Column {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DesktopScrobbleType.values().forEach { type ->
                val active = type == selected
                Text(
                    text = when (type) {
                        DesktopScrobbleType.Simple -> ds("settings.scrobble.simple")
                        DesktopScrobbleType.NowPlaying -> ds("settings.scrobble.now_playing")
                    },
                    color = if (active) Color.Black else Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) RiPlayAccent else Color.White.copy(alpha = 0.08f))
                        .clickable { onSelect(type) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun AccountStatusLine(text: String, isError: Boolean) {
    Text(
        text = text,
        color = if (isError) Color(0xFFFF7A7A) else RiPlayAccent,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SEZIONE WEBDAV
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WebDavAccountsSection() {
    val coroutineScope = rememberCoroutineScope()
    val accounts by DesktopWebDavManager.accounts.collectAsState()
    val testState by DesktopWebDavManager.testConnectionState.collectAsState()
    val syncState by DesktopWebDavManager.syncState.collectAsState()
    val backupState by DesktopWebDavManager.backupState.collectAsState()
    val webDavEnabled = DesktopAccountsStore.webDavEnabled

    var formVisible by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<WebDavAccount?>(null) }
    var restoreInfo by remember { mutableStateOf<WebDavBackupInfo?>(null) }
    var restoreBusy by remember { mutableStateOf(false) }
    var restoreStatus by remember { mutableStateOf<String?>(null) }
    var restoreOk by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { DesktopWebDavManager.refreshAccounts() }
    LaunchedEffect(Unit) {
        DesktopWebDavManager.messages.collect { (msg, err) -> message = msg; messageIsError = err }
    }

    AccountSectionTitle(ds("settings.section.webdav"))
    AccountCard {
        AccountSwitchRow(
            title = ds("settings.webdav.enable"),
            description = ds("settings.webdav.enable_desc"),
            checked = webDavEnabled,
            onChecked = { DesktopAccountsStore.updateWebDavEnabled(it) }
        )

        AnimatedVisibility(visible = webDavEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // ELENCO ACCOUNT
                accounts.forEach { account ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .clickable {
                                editingAccount = account
                                DesktopWebDavManager.resetTestState()
                                formVisible = true
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = account.name.ifBlank { account.baseUrl },
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (account.isMusicSource)
                                        ds("settings.webdav.badge_music_source")
                                    else
                                        ds("settings.webdav.badge_backup"),
                                    color = RiPlayAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(RiPlayAccent.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = account.baseUrl,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                        // CANCELLA
                        Text(
                            text = ds("settings.webdav.delete"),
                            color = Color(0xFFFF7A7A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { DesktopWebDavManager.deleteAccount(account) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                // AGGIUNGI ACCOUNT
                if (!formVisible) {
                    AccountActionButton(
                        label = ds("settings.webdav.add_account"),
                        emphasized = true,
                        onClick = {
                            editingAccount = null
                            DesktopWebDavManager.resetTestState()
                            formVisible = true
                        }
                    )
                }

                // FORM ACCOUNT (replica desktop di WebDavAccountFormSheet)
                AnimatedVisibility(visible = formVisible) {
                    WebDavAccountFormPanel(
                        isEditing = editingAccount != null,
                        account = editingAccount ?: webDavAccountEmpty(),
                        onDismiss = {
                            formVisible = false
                            editingAccount = null
                        }
                    )
                }

                // SINCRONIZZAZIONE MUSICA (sorgenti musicali)
                AnimatedVisibility(visible = accounts.any { it.isMusicSource }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AccountActionButton(
                            label = ds("settings.webdav.sync_music"),
                            onClick = { DesktopWebDavManager.scanMusicFolders() }
                        )
                        when (val s = syncState) {
                            is DesktopWebDavManager.SyncState.Loading -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Text(
                                    ds("settings.webdav.sync_loading"),
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                            is DesktopWebDavManager.SyncState.Success -> AccountStatusLine(
                                ds("settings.webdav.sync.done", "songs" to s.songs, "folders" to s.folders),
                                isError = false
                            )
                            is DesktopWebDavManager.SyncState.Error -> AccountStatusLine(
                                ds("settings.webdav.sync.failed", "error" to s.message),
                                isError = true
                            )
                            DesktopWebDavManager.SyncState.Idle -> {}
                        }
                    }
                }

                // BACKUP / RESTORE DATABASE (account di backup)
                AnimatedVisibility(visible = accounts.any { !it.isMusicSource }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccountActionButton(
                                label = ds("settings.webdav.backup_db"),
                                onClick = {
                                    accounts.firstOrNull { !it.isMusicSource }?.let {
                                        DesktopWebDavManager.backupDatabaseToWebDav(it)
                                    }
                                }
                            )
                            AccountActionButton(
                                label = ds("settings.webdav.restore_db"),
                                enabled = !restoreBusy,
                                onClick = {
                                    coroutineScope.launch {
                                        restoreBusy = true
                                        restoreStatus = null
                                        val account = accounts.firstOrNull { !it.isMusicSource }
                                        restoreInfo =
                                            account?.let { DesktopWebDavManager.fetchBackupInfo(it) }
                                        restoreBusy = false
                                    }
                                }
                            )
                        }
                        backupState?.let {
                            Text(
                                text = it,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // CONFERMA RESTORE (replica di WebDavDatabaseRestoreConfirmDialog)
                restoreInfo?.let { info ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RiPlayAccent.copy(alpha = 0.08f))
                            .border(1.dp, RiPlayAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            ds("settings.webdav.restore_confirm_title"),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = ds("settings.webdav.restore_confirm_info") +
                                    "\n" + ds(
                                "settings.webdav.restore_confirm_stamp",
                                "date" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                                    .format(java.util.Date(info.timestamp)),
                                "device" to (info.deviceName ?: "?")
                            ),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccountActionButton(
                                label = ds("settings.webdav.restore_confirm_yes"),
                                emphasized = true,
                                onClick = {
                                    coroutineScope.launch {
                                        restoreInfo = null
                                        restoreBusy = true
                                        val account = accounts.firstOrNull { !it.isMusicSource }
                                        val result = account?.let {
                                            DesktopWebDavManager.restoreDatabaseFromWebDav(it)
                                        }
                                        restoreBusy = false
                                        if (result == null) {
                                            restoreOk = true
                                            restoreStatus = ds("settings.webdav.restore_scheduled")
                                        } else {
                                            restoreOk = false
                                            restoreStatus = result
                                        }
                                    }
                                }
                            )
                            AccountActionButton(
                                label = ds("settings.webdav.restore_confirm_no"),
                                onClick = { restoreInfo = null }
                            )
                        }
                    }
                }

                if (restoreBusy) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Text(
                            ds("settings.webdav.restore_loading"),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }

                restoreStatus?.let { AccountStatusLine(it, isError = !restoreOk) }

                message?.let {
                    AccountStatusLine(it, isError = messageIsError)
                    LaunchedEffect(it) {
                        kotlinx.coroutines.delay(6000)
                        message = null
                    }
                }
            }
        }
    }
}

/**
 * Form di un account WebDAV (replica desktop di WebDavAccountFormSheet
 * Android): campi nome/server/utente/password/cartella, switch sorgente
 * musicale + sottocartelle, TEST CONNESSIONE e SALVA (abilitato solo dopo
 * un test riuscito, password cifrata con DesktopCryptoManager).
 */
@Composable
private fun WebDavAccountFormPanel(
    isEditing: Boolean,
    account: WebDavAccount,
    onDismiss: () -> Unit
) {
    var name by remember(account.id) { mutableStateOf(account.name) }
    var url by remember(account.id) { mutableStateOf(account.baseUrl) }
    var user by remember(account.id) { mutableStateOf(account.username) }
    var pass by remember(account.id) {
        mutableStateOf(
            if (isEditing) DesktopCryptoManager.decrypt(account.encryptedPassword)
            else account.encryptedPassword
        )
    }
    var folder by remember(account.id) { mutableStateOf(account.remoteFolder) }
    var isMusicSource by remember(account.id) { mutableStateOf(account.isMusicSource) }
    var scanSubFolders by remember(account.id) { mutableStateOf(account.scanSubFolders) }
    var passwordVisible by remember { mutableStateOf(false) }

    val testState by DesktopWebDavManager.testConnectionState.collectAsState()
    val testing = testState is DesktopWebDavManager.TestConnectionState.Testing
    val testOk = testState is DesktopWebDavManager.TestConnectionState.Success

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        Text(
            text = ds(if (isEditing) "settings.webdav.form.title_edit" else "settings.webdav.form.title_add"),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = ds(
                if (isMusicSource) "settings.webdav.form.hint_music"
                else "settings.webdav.form.hint_backup"
            ),
            color = RiPlayAccent,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        AccountTextFieldRow(ds("settings.webdav.form.name"), name, { name = it })
        AccountTextFieldRow(ds("settings.webdav.form.url"), url, { url = it })
        AccountTextFieldRow(ds("settings.webdav.form.username"), user, { user = it })

        // PASSWORD con mostra/nascondi (come WebDavAccountFormSheet)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AccountTextFieldRow(
                    label = ds(
                        if (isEditing) "settings.webdav.form.password_edit"
                        else "settings.webdav.form.password_add"
                    ),
                    value = pass,
                    onValueChange = { pass = it },
                    isPassword = true,
                    showValue = passwordVisible
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = ds(
                    if (passwordVisible) "settings.webdav.form.hide_password"
                    else "settings.webdav.form.show_password"
                ),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { passwordVisible = !passwordVisible }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        AccountTextFieldRow(
            ds("settings.webdav.form.folder"),
            folder,
            { folder = it },
            placeholder = ds("settings.webdav.form.folder_placeholder")
        )

        // SORGENTE MUSICALE (true = musica, false = solo backup)
        AccountSwitchRow(
            title = ds("settings.webdav.form.music_source"),
            description = ds("settings.webdav.form.music_source_desc"),
            checked = isMusicSource,
            onChecked = { isMusicSource = it }
        )

        AnimatedVisibility(visible = isMusicSource) {
            AccountSwitchRow(
                title = ds("settings.webdav.form.subfolders"),
                description = ds("settings.webdav.form.subfolders_desc"),
                checked = scanSubFolders,
                onChecked = { scanSubFolders = it }
            )
        }

        // TEST CONNESSIONE
        AccountActionButton(
            label = if (testing) ds("settings.webdav.test.testing") else ds("settings.webdav.test.button"),
            enabled = !testing && !testOk,
            emphasized = false,
            onClick = {
                DesktopWebDavManager.testConnection(
                    account.copy(
                        name = name,
                        baseUrl = url,
                        username = user,
                        encryptedPassword = DesktopCryptoManager.encrypt(pass),
                        remoteFolder = folder,
                        isMusicSource = isMusicSource,
                        scanSubFolders = scanSubFolders
                    )
                )
            }
        )

        when (val t = testState) {
            is DesktopWebDavManager.TestConnectionState.Success ->
                AccountStatusLine(t.message, isError = false)
            is DesktopWebDavManager.TestConnectionState.Error ->
                AccountStatusLine(t.message, isError = true)
            else -> {}
        }

        // SALVA (solo dopo test riuscito: stessa regola dell'Android)
        AccountActionButton(
            label = ds("settings.webdav.form.save"),
            emphasized = true,
            enabled = testOk,
            onClick = {
                DesktopWebDavManager.saveAccount(
                    account.copy(
                        name = name.ifBlank { url },
                        baseUrl = url,
                        username = user,
                        encryptedPassword = DesktopCryptoManager.encrypt(pass),
                        remoteFolder = folder,
                        isMusicSource = isMusicSource,
                        scanSubFolders = scanSubFolders
                    )
                )
                onDismiss()
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SEZIONE LAST.FM
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LastFmAccountsSection() {
    val store = DesktopAccountsStore
    var loginActive by remember { mutableStateOf(false) }

    AccountSectionTitle(ds("settings.section.lastfm"))
    AccountCard {
        AccountSwitchRow(
            title = ds("settings.lastfm.enable"),
            description = ds("settings.lastfm.enable_desc"),
            checked = store.lastFmEnabled,
            onChecked = { store.updateLastFmEnabled(it) }
        )

        AnimatedVisibility(visible = store.lastFmEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // CONNETTI / DISCONNETTI
                if (!loginActive) {
                    AccountActionButton(
                        label = if (store.lastFmSessionKey.isNotBlank())
                            ds("settings.lastfm.disconnect")
                        else ds("settings.lastfm.connect"),
                        emphasized = store.lastFmSessionKey.isBlank(),
                        onClick = {
                            if (store.lastFmSessionKey.isNotBlank()) {
                                store.updateLastFmSessionKey("")
                            } else {
                                loginActive = true
                            }
                        }
                    )
                    if (store.lastFmSessionKey.isNotBlank()) {
                        Text(
                            text = ds("settings.lastfm.connected"),
                            color = RiPlayAccent,
                            fontSize = 12.sp
                        )
                    }
                }

                // AREA DI LOGIN (browser integrato, come LastFmAuthScreen Android)
                AnimatedVisibility(visible = loginActive) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesktopLastFmAuthPanel(
                            onAuthSuccess = { loginActive = false }
                        )
                        AccountActionButton(
                            label = ds("common.cancel"),
                            onClick = { loginActive = false }
                        )
                    }
                }

                // TIPO DI SCROBBLE
                ScrobbleTypeChips(
                    title = ds("settings.lastfm.scrobble_type"),
                    selected = store.lastFmScrobbleType,
                    onSelect = { store.updateLastFmScrobbleType(it) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SEZIONE LISTENBRAINZ
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ListenBrainzAccountsSection() {
    val store = DesktopAccountsStore
    val coroutineScope = rememberCoroutineScope()
    var loginActive by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    var serverInput by remember(store.listenBrainzServerUrl) { mutableStateOf(store.listenBrainzServerUrl) }
    var showToken by remember { mutableStateOf(false) }
    var validating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AccountSectionTitle(ds("settings.section.listenbrainz"))
    AccountCard {
        AccountSwitchRow(
            title = ds("settings.listenbrainz.enable"),
            description = ds("settings.listenbrainz.enable_desc"),
            checked = store.listenBrainzEnabled,
            onChecked = { store.updateListenBrainzEnabled(it) }
        )

        AnimatedVisibility(visible = store.listenBrainzEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                if (!loginActive) {
                    // CONNETTI / DISCONNETTI
                    AccountActionButton(
                        label = if (store.listenBrainzToken.isNotBlank())
                            ds("settings.listenbrainz.disconnect")
                        else ds("settings.listenbrainz.connect"),
                        emphasized = store.listenBrainzToken.isBlank(),
                        onClick = {
                            if (store.listenBrainzToken.isNotBlank()) {
                                store.updateListenBrainzToken("")
                                store.updateListenBrainzUsername("")
                                error = null
                            } else {
                                tokenInput = ""
                                error = null
                                loginActive = true
                            }
                        }
                    )
                    if (store.listenBrainzToken.isNotBlank()) {
                        Text(
                            text = ds("settings.listenbrainz.connected"),
                            color = RiPlayAccent,
                            fontSize = 12.sp
                        )
                        if (store.listenBrainzUsername.isNotBlank()) {
                            Text(
                                text = ds("settings.listenbrainz.connected_as", "user" to store.listenBrainzUsername),
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // FORM TOKEN (replica desktop di ListenBrainzAuthScreen Android)
                AnimatedVisibility(visible = loginActive) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        Text(
                            text = ds("settings.listenbrainz.token_instructions"),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        AccountTextFieldRow(
                            ds("settings.listenbrainz.token_hint"),
                            tokenInput,
                            { tokenInput = it },
                            isPassword = true,
                            showValue = showToken
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ds(
                                    if (showToken) "settings.webdav.form.hide_password"
                                    else "settings.webdav.form.show_password"
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showToken = !showToken }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = ds("settings.listenbrainz.get_token"),
                                color = RiPlayAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        runCatching {
                                            java.awt.Desktop.getDesktop()
                                                .browse(java.net.URI("https://listenbrainz.org/settings/"))
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }

                        // SERVER CUSTOM (vuoto = ufficiale)
                        AccountTextFieldRow(
                            ds("settings.listenbrainz.server_hint"),
                            serverInput,
                            { serverInput = it },
                            placeholder = ds("settings.listenbrainz.server_official")
                        )

                        AccountActionButton(
                            label = if (validating)
                                ds("settings.listenbrainz.validating")
                            else ds("settings.listenbrainz.connect"),
                            emphasized = !validating,
                            enabled = !validating && tokenInput.isNotBlank(),
                            onClick = {
                                if (tokenInput.isBlank()) return@AccountActionButton
                                validating = true
                                error = null
                                coroutineScope.launch {
                                    val serverToSave = serverInput.trim()
                                    val tokenToSave = tokenInput.trim()
                                    val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        // Come ListenBrainzAuthScreen Android: il service
                                        // viene puntato al server scelto, poi il token
                                        // viene validato (solo se valido si persiste)
                                        runCatching {
                                            DesktopListenBrainzClient
                                                .service(serverToSave)
                                                .validateToken(tokenToSave)
                                                .getOrThrow()
                                        }
                                    }
                                    validating = false
                                    result.onSuccess { response ->
                                        if (response.valid) {
                                            store.updateListenBrainzToken(tokenToSave)
                                            store.updateListenBrainzUsername(response.userName ?: "")
                                            store.updateListenBrainzServerUrl(serverToSave)
                                            loginActive = false
                                        } else {
                                            error = ds("settings.listenbrainz.token_invalid")
                                        }
                                    }.onFailure { e ->
                                        println("[RiPlay-ListenBrainz] validazione fallita: ${e.message}")
                                        error = ds("settings.listenbrainz.token_invalid")
                                    }
                                }
                            }
                        )

                        error?.let { AccountStatusLine(it, isError = true) }

                        AccountActionButton(
                            label = ds("common.cancel"),
                            onClick = { loginActive = false }
                        )
                    }
                }

                // SERVER VISIBILE ANCHE DA CONNESSO (come TextDialogSettingEntry Android)
                if (!loginActive) {
                    AccountTextFieldRow(
                        ds("settings.listenbrainz.server_hint"),
                        serverInput,
                        { serverInput = it; store.updateListenBrainzServerUrl(it.trim()) },
                        placeholder = ds("settings.listenbrainz.server_official")
                    )
                }

                // TIPO DI SCROBBLE
                ScrobbleTypeChips(
                    title = ds("settings.listenbrainz.scrobble_type"),
                    selected = store.listenBrainzScrobbleType,
                    onSelect = { store.updateListenBrainzScrobbleType(it) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SEZIONE DISCORD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DiscordAccountsSection() {
    val store = DesktopAccountsStore
    var loginActive by remember { mutableStateOf(false) }

    AccountSectionTitle(ds("settings.section.discord"))
    AccountCard {
        AccountSwitchRow(
            title = ds("settings.discord.enable"),
            description = ds("settings.discord.enable_desc"),
            checked = store.discordEnabled,
            onChecked = { store.updateDiscordEnabled(it) }
        )

        AnimatedVisibility(visible = store.discordEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // CONNETTI / DISCONNETTI
                if (!loginActive) {
                    AccountActionButton(
                        label = if (store.discordToken.isNotBlank())
                            ds("settings.discord.disconnect")
                        else ds("settings.discord.connect"),
                        emphasized = store.discordToken.isBlank(),
                        onClick = {
                            if (store.discordToken.isNotBlank()) {
                                store.setDiscordLogin("", "", "")
                            } else {
                                loginActive = true
                            }
                        }
                    )
                }

                // INFO ACCOUNT (username + avatar)
                if (store.discordToken.isNotBlank() && !loginActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (store.discordUserAvatar.isNotBlank()) {
                            AsyncImage(
                                model = store.discordUserAvatar,
                                contentDescription = null,
                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(17.dp))
                            )
                        }
                        Text(
                            text = if (store.discordUserName.isNotBlank())
                                ds("settings.discord.connected_as", "user" to store.discordUserName)
                            else ds("settings.discord.connected"),
                            color = RiPlayAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // AREA DI LOGIN (browser integrato + token manuale)
                AnimatedVisibility(visible = loginActive) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesktopDiscordLoginPanel(
                            onGetToken = { token, username, avatar ->
                                store.setDiscordLogin(token, username, avatar)
                            },
                            onDismiss = { loginActive = false }
                        )
                        AccountActionButton(
                            label = ds("common.cancel"),
                            onClick = { loginActive = false }
                        )
                    }
                }
            }
        }
    }
}

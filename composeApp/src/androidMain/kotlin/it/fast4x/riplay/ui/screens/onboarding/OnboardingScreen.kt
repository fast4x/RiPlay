package it.fast4x.riplay.ui.screens.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import it.fast4x.riplay.R
import org.jetbrains.compose.resources.vectorResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onAllPermissionsGranted: () -> Unit, // Callback quando l'utente clicca "Inizia"
) {
    val context = LocalContext.current

    // Stato per forzare la ricomposizione quando lo stato dei permessi cambia
    // In un'app reale useresti un ViewModel, qui usiamo remember per semplicità
    var refreshTrigger by remember { mutableStateOf(0) }

    // Helper per controllare lo stato
    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Definizione degli elementi (La lista viene ricalcolata quando refreshTrigger cambia)
    val items by remember(refreshTrigger) {
        derivedStateOf {
            buildList {

                add(
                    OnboardingSection(
                        title = "Permessi"
                    )
                )

                // 1. Notifiche (Richiesto su Android 13+)
                val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    checkPermission(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    true // Su Android < 13 è concesso di default col manifest
                }

                add(
                    OnboardingItem(
                        id = "notifications",
                        title = "Notifiche",
                        description = "Ricevi notifiche quando cambi canzone o controlli la musica in background.",
                        icon = R.drawable.notifications,
                        status = if (notificationGranted) PermissionStatus.GRANTED else PermissionStatus.NOT_REQUESTED,
                        onRequest = {
                            // Qui inietteresti il launcher: notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            // Per ora simuliamo il cambio stato
                            refreshTrigger++
                        }
                    )
                )

                // 2. Memorizzazione (Storage / Lettura File)
                // Nota: Su Android 13+ READ_MEDIA_AUDIO è preferito a READ_EXTERNAL_STORAGE
                val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    checkPermission(Manifest.permission.READ_MEDIA_AUDIO)
                } else {
                    checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

                add(
                    OnboardingItem(
                        id = "storage",
                        title = "Accesso File Musicali",
                        description = "Permetti all'app di leggere la libreria musicale dal tuo dispositivo.",
                        icon = R.drawable.folder,
                        status = if (storageGranted) PermissionStatus.GRANTED else PermissionStatus.NOT_REQUESTED,
                        onRequest = {
                            // storagePermissionLauncher.launch(...)
                            refreshTrigger++
                        }
                    )
                )

                // 3. Bluetooth (Per la logica delle cuffie)
                val btGranted = checkPermission(Manifest.permission.BLUETOOTH_CONNECT)

                add(
                    OnboardingItem(
                        id = "bluetooth",
                        title = "Bluetooth",
                        description = "Permetti all'app di rilevare le cuffie per mettere in pausa/riprendere la musica.",
                        icon = R.drawable.ui,
                        status = if (btGranted) PermissionStatus.GRANTED else PermissionStatus.NOT_REQUESTED,
                        onRequest = {
                            // btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            refreshTrigger++
                        }
                    )
                )

                // 4. Ottimizzazione Batteria (Non è un permesso, ma un intent)
                // Verifichiamo se siamo nella lista delle eccezioni (semplificato)
                val isIgnoringBattery = true // Qui servirebbe un PowerManager check reale

                add(
                    OnboardingItem(
                        id = "battery",
                        title = "Ottimizzazione Batteria",
                        description = "Evita che il sistema arresti la musica quando lo schermo è spento.",
                        icon = R.drawable.chevron_back,
                        status = if (isIgnoringBattery) PermissionStatus.GRANTED else PermissionStatus.NOT_REQUESTED,
                        onRequest = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                            // Nota: non possiamo sapere subito se l'utente ha cliccato "Permetti",
                            // quindi in una app reale useresti un ActivityResultLauncher per controllare il risultato.
                        }
                    )
                )

                add(
                    OnboardingItem(
                        id = "battery",
                        title = "Ottimizzazione Batteria",
                        description = "Evita che il sistema arresti la musica quando lo schermo è spento.",
                        icon = R.drawable.chevron_back,
                        status = if (isIgnoringBattery) PermissionStatus.GRANTED else PermissionStatus.NOT_REQUESTED,
                        onRequest = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                            // Nota: non possiamo sapere subito se l'utente ha cliccato "Permetti",
                            // quindi in una app reale useresti un ActivityResultLauncher per controllare il risultato.
                        }
                    )
                )

                add(
                    OnboardingItem(
                        id = "battery",
                        title = "Ottimizzazione Batteria",
                        description = "Evita che il sistema arresti la musica quando lo schermo è spento.",
                        icon = R.drawable.chevron_back,
                        status = if (isIgnoringBattery) PermissionStatus.GRANTED else PermissionStatus.NOT_REQUESTED,
                        onRequest = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                            // Nota: non possiamo sapere subito se l'utente ha cliccato "Permetti",
                            // quindi in una app reale useresti un ActivityResultLauncher per controllare il risultato.
                        }
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RiPlay") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Illustrazione o Icona grande
            Icon(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Configura la tua esperienza musicale perfetta",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Consenti queste opzioni per sfruttare al meglio tutte le funzionalità dell'app.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items) { item ->
                    when (item) {
                        is OnboardingItem -> PermissionCard(item = item)
                        is OnboardingSection -> PermissionSection(item = item)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pulsante finale
            Button(
                onClick = { onAllPermissionsGranted() },
                modifier = Modifier.fillMaxWidth(),
                enabled = true //items.all { it.status == PermissionStatus.GRANTED }
            ) {
                Text("Inizia")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSection(item: OnboardingSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionCard(item: OnboardingItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icona a sinistra
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Testo centrale
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pulsante Azione a destra
            ActionButton(status = item.status, onClick = item.onRequest)
        }
    }
}

@Composable
fun ActionButton(status: PermissionStatus, onClick: () -> Unit) {
    when (status) {
        PermissionStatus.GRANTED -> {
            Icon(
                painter = painterResource(id = R.drawable.checkmark),
                contentDescription = "Concesso",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        PermissionStatus.NOT_REQUESTED, PermissionStatus.DENIED -> {
            OutlinedButton(onClick = onClick) {
                Text("Concedi")
            }
        }
        PermissionStatus.PERMANENTLY_DENIED -> {
            TextButton(onClick = onClick) {
                Text("Impostazioni")
            }
        }
    }
}
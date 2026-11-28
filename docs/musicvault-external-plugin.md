# Music Vault con delega al plugin esterno (protocollo "MusicVault" v1)

> Stato: implementato su `feature/musicvault-plugin` (base `v0.8.8-rc.4`)
> Autore strategia: sessione di sviluppo 2026-11

## 1. Perche'

Fino al tag `v0.8.8-rc.4` il download della Music Vault avveniva **dentro** RiPlay:

```
MusicVaultButton -> MusicVaultService.enqueue -> WorkManager -> MusicVaultWorker
    -> Chaquopy (Python embedded) + yt-dlp  (modules/chaquopy, MusicVault.py)
    -> moveToUserFolder -> DB (stato) -> MusicVaultEvents
```

Questa architettura ha costi strutturali:

- **APK gonfio**: il runtime Python 3.13 + yt-dlp (solo arm64-v8a/x86_64) e i
  relativi binari sono inclusi nel build `full`;
- **ABI limitate**: i dispositivi 32 bit (molti TV box) non possono usare la
  funzione e ricevono una degradazione controllata;
- **Accoppiamento**: ogni problema di yt-dlp (blocchi YouTube, rotte extractor)
  richiede un aggiornamento di RiPlay;
- **Manutenzione**: test/fix del downloader rallentano le release del player.

La strategia scelta: **delegare l'esecuzione a una applicazione esterna**
("plugin") che RiPlay richiama tramite un protocollo Android standard
(Intent/Service/Broadcast), mantenendo identica l'esperienza utente lato
RiPlay (pulsante Music Vault, stati, cartella Vault).

## 2. Ripartizione delle responsabilita'

RiPlay e plugin hanno ruoli netti e non sovrapposti:

| Responsabilita' | Dove vive |
|---|---|
| Flag attivazione/disattivazione (unico controllo in RiPlay) | RiPlay (`musicVaultEnabled`) |
| Rilevamento del plugin installato | RiPlay (`VaultPluginClient`) |
| Stati del vault nel DB (`NONE/QUEUED/DOWNLOADING/...`) | RiPlay |
| Cartella Vault e import file | RiPlay |
| **Formato audio/video** | **plugin** (`VaultPrefs`, UI dedicata) |
| Permessi ("Tutti i file", notifiche) | **plugin** |
| Runtime Python/yt-dlp, coda, notifiche di lavoro | **plugin** |

L'app plugin di riferimento si chiama **MusicVault** (package
`com.musicvault.app`): nessun riferimento a RiPlay in nomi di file, classi o
package del plugin, che e' un'app autonoma riutilizzabile da qualunque host
che parli il protocollo.

## 3. Architettura dopo la migrazione

```
RiPlay                                        MusicVault (plugin)
-------                                       -------------------
MusicVaultButton (solo se flag ON             VaultService (foreground)
  e plugin rilevato)                            coda seriale per songId
  | startForegroundService(ACTION_ENQUEUE) -->  PythonEngine (Chaquopy)
  | (stato DB = QUEUED)                           yt-dlp: formato impostato nel plugin
  |                                               thumbnail + metadati
  | <-- broadcast esplicito STARTED -----------  (stato DB = DOWNLOADING)
  | <-- broadcast esplicito COMPLETED -----------  file nella cartella Vault
  |      (updateMusicVaultCompleted,               o FileProvider (handoff)
  |       MusicVaultEvents.Completed)
  | startService(ACTION_CANCEL) --------------->  rimozione job / file parziali
```

Componenti RiPlay (flavor `full`, package `it.fast4x.riplay.musicvault`):

| File | Ruolo |
|---|---|
| `VaultPluginContract.kt` | Costanti del protocollo (azioni, extras, versioni) |
| `VaultPluginClient.kt` | Rilevamento plugin (`queryIntentServices`), dispatch enqueue/cancel, disponibilita' con cache |
| `VaultPluginResultReceiver.kt` | Receiver manifest degli esiti; import file in modalita' handoff |
| `MusicVaultService.kt` | Orchestrazione senza WorkManager |
| `MusicVaultButton.kt` | Stati visuali pilotati dal solo DB; visibile solo con plugin presente |
| `MusicVaultUtils.kt` | `initializeMusicVault` (diagnostica) + `isMusicVaultPluginAvailable` per la UI |

Rimosso: `modules/chaquopy`, `MusicVaultWorker`, interfaccia `PythonEngine`,
dipendenze/proguard correlate. WorkManager resta in RiPlay per gli altri usi
(job MusicBrainz ecc.), non piu' per il vault.

Il flavor `foss` non cambia (Music Vault resta disabilitato come oggi).

## 4. Protocollo (v1)

RiPlay -> plugin: `startForegroundService` / `startService` **espliciti** verso il
Service esportato che dichiara `com.musicvault.action.ENQUEUE`
(RiPlay e' in primo piano quando l'utente preme il pulsante, quindi la chiamata
e' ammessa anche su Android 12+).

| Azione | Extra |
|---|---|
| `...action.ENQUEUE` | `protocol_version` (int), `request_id` (UUID), `song_id`, `url`, `title`, `artist`, `target_dir` (path assoluto), `target_uri` (SAF, opz.), `delivery` (`direct`\|`handoff`), `callback_package` |
| `...action.CANCEL` | `protocol_version`, `song_id`, `callback_package` |

Il **formato NON e' parte della richiesta**: e' una impostazione locale del
plugin (Audio = `bestaudio/best`, Video = `best[ext=mp4]/best` pre-mergiato
perche' yt-dlp in-process non ha ffmpeg), scelta nella UI del plugin e
persistita in `VaultPrefs`.

Plugin -> RiPlay: broadcast **espliciti** (`setPackage(callback_package)`) verso
`VaultPluginResultReceiver` (exported, manifest `androidFull`).

| Azione | Extra |
|---|---|
| `...action.RESULT_STARTED` | `protocol_version`, `request_id`, `song_id` |
| `...action.RESULT_PROGRESS` | + `progress` (0..100, opzionale) |
| `...action.RESULT_COMPLETED` | `song_id`, `request_id`; direct: `file_name`, `thumbnail_file_name`, `result_title`, `result_artist`, `duration_seconds`, `file_path`; handoff: `file_uri` + `thumbnail_uri` (FileProvider, grant via `grantUriPermission`) |
| `...action.RESULT_FAILED` | + `error_message` |

### Modalita' di consegna del file

- **direct** (cartella Vault scelta dall'utente: pubblica o SAF risolta):
  il plugin scrive direttamente in `target_dir` e restituisce i nomi file;
- **handoff** (cartella Vault di default = `filesDir` privato di RiPlay, o
  qualunque fallimento di scrittura diretta): il plugin scarica nel proprio
  storage, espone il file via FileProvider, chiama `grantUriPermission` verso
  RiPlay e RiPlay importa il file nella cartella Vault (stessa logica di
  `moveToUserFolder` del vecchio worker: SAF -> DocumentFile, altrimenti file).

### Stati Music Vault (invariati)

`NONE -> QUEUED (enqueue) -> DOWNLOADING (RESULT_STARTED) -> COMPLETED | FAILED`
con `FILE_MISSING` gestito dal pulsante come prima. Gli aggiornamenti del DB
sono "guarded": valgono solo se lo stato corrente e' QUEUED/DOWNLOADING, cosi'
le risposte di richieste annullate vengono ignorate.

## 5. Rilevamento del plugin e UI in RiPlay

1. RiPlay cerca i Service che dichiarano `com.musicvault.action.ENQUEUE`
   (`<queries>` nel manifest `androidFull` + `QUERY_ALL_PACKAGES` gia' presente);
2. `VaultPluginClient.isPluginAvailable()` espone la disponibilita' con una
   cache di 5 secondi (il check si puo' chiamare anche da composables hot);
3. **Impostazioni**: la sezione Music Vault contiene SOLO il flag di
   attivazione/disattivazione, titolato con l'unica stringa conservata
   (`settings_music_vault_title`), ed e' visibile **solo se un plugin
   compatibile e' installato**;
4. il pulsante Music Vault sui brani segue la stessa regola (flag ON + plugin
   presente), quindi con il plugin disinstallato la funzione sparisce senza
   errori.

Tutte le altre stringhe di traduzione che facevano riferimento a Music Vault
(disclaimer, descrizioni, scelta plugin, formato) sono state eliminate da
tutte le locale: la configurazione e i testi correlati vivono nell'app plugin.

## 6. Limiti e note operative

- **Annullamento**: la chiamata Python yt-dlp non e' interrompibile; un lavoro
  in corso "annullato" viene lasciato finire dal plugin ma i file prodotti
  vengono eliminati e nessun esito viene notificato (stato RiPlay torna NONE).
- **Dispositivi 32 bit**: ora funzionano, perche' il Python gira nel processo
  del plugin, che puo' pubblicare i propri ABI (RiPlay non contiene piu' Python).
- **Android 15**: il foreground service `dataSync` del plugin ha il limite di
  ~6h/giorno; per code molto lunghe documentare nel plugin.
- **Sicurezza**: il protocollo e' aperto a qualunque app che dichiari l'azione
  (scelta esplicita di progetto); il receiver RiPlay valida la versione del
  protocollo e lo stato del brano prima di toccare il DB. Un plugin puo'
  inviare broadcast solo verso package che conosce (`callback_package` e'
  firmato dalla richiesta).
- **FOSS**: la variante `foss` resta senza Music Vault (stub). Volendo, il
  protocollo consentirebbe di abilitare il Vault anche in FOSS senza Python
  embedded: e' un'estensione futura deliberatamente non inclusa.

## 7. App plugin di riferimento

`MusicVault` (progetto Gradle standalone, package `com.musicvault.app`):
implementa il protocollo v1 con la logica yt-dlp di `MusicVault.py` (formati
audio/video selezionabili nella sua UI, thumbnail, outtmpl
`%(title)s [%(id)s].%(ext)s`, stessi header e player_client), foreground
service con notifica di stato, UI minimale (stato runtime, permesso
"Tutti i file", formato, log eventi). Specifica completa nel `PROTOCOL.md`
del progetto.

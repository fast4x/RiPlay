package it.fast4x.riplay.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Collections
import java.util.jar.JarFile


object DesktopUpdateChecker {

    const val VER_URL =
        "https://raw.githubusercontent.com/fast4x/RiPlay/main/updatedVersion/updatedVersionCode.ver"
    const val RELEASES_URL = "https://github.com/fast4x/RiPlay/releases/latest"

    /** esito del controllo (la UI lo mostra in About e all'avvio) */
    sealed interface UpdateCheckResult {
        data object Checking : UpdateCheckResult
        data object UpToDate : UpdateCheckResult
        data class Available(
            val versionCode: Int,
            val versionName: String,
            val productName: String
        ) : UpdateCheckResult
        data object Failed : UpdateCheckResult
    }

    /**
     * origine della versione locale rilevata a runtime (utile per log/diagnostica):
     * - JPACKAGE_PROPERTY     : proprieta' di sistema "jpackage.app-version"
     *                           passata alla JVM dal launcher tramite la riga
     *                           java-options del .cfg (presente quando l'app
     *                           parte dall'eseguibile dell'installazione)
     * - JPACKAGE_INFO_FILE    : file di info generato da jpackage nella cartella
     *                           "app" dell'installazione: <launcher>.cfg
     *                           (RiPlay.cfg: SEMPRE presente, msi e zip) oppure
     *                           .jpackage.xml (presente solo nelle distribuzioni
     *                           zip/app-image)
     * - JAR_MANIFEST          : attributo Implementation-Version (o App-Version)
     *                           del manifest del jar applicativo
     * - COMPILE_TIME_FALLBACK : costante DesktopAppInfo.VERSION (ultima spiaggia)
     */
    enum class LocalVersionSource(val label: String) {
        JPACKAGE_PROPERTY("proprieta' jpackage.app-version (launcher jpackage)"),
        JPACKAGE_INFO_FILE("file info jpackage (RiPlay.cfg / .jpackage.xml)"),
        JAR_MANIFEST("manifest del jar applicativo"),
        COMPILE_TIME_FALLBACK("costante di build DesktopAppInfo.VERSION (fallback)")
    }

    /** versione locale risolta + il punto da cui e' stata letta */
    data class LocalVersionInfo(
        val version: String,
        val source: LocalVersionSource,
        val file: File? = null
    ) {
        /** descrizione compatta per About/Impostazioni, es.
         *  "0.8.4 (file info jpackage (RiPlay.cfg / .jpackage.xml): RiPlay.cfg)" */
        fun describe(): String = when (file) {
            null -> "$version (${source.label})"
            else -> "$version (${source.label}: ${file.name})"
        }
    }

    // ------------------------------------------------------------------
    //  Versione locale dinamica: cache rinfrescabile + diagnostica sonde
    // ------------------------------------------------------------------

    @Volatile
    private var localVersionCache: LocalVersionInfo? = null

    /** log delle sonde effettuate (stampato in console + via diagnostics()) */
    private val probes: MutableList<String> = Collections.synchronizedList(ArrayList<String>())

    private fun probe(message: String) {
        val line = "[RiPlay-Update] $message"
        probes.add(line)
        println(line)
    }

    /**
     * elenco delle sonde effettuate per rilevare la versione locale (anchor
     * provati, cartelle esaminate, esiti): utile per il supporto e per
     * mostrare/diagnosticare il rilevamento direttamente in Impostazioni.
     */
    fun diagnostics(): List<String> = probes.toList()

    /**
     * Versione locale dell'applicativo, risolta DINAMICAMENTE a runtime dai
     * file dell'installazione (al primo accesso, poi in cache; rinfrescabile
     * con refreshLocalVersion()). Ordine di risoluzione:
     *
     *  1) proprieta' "jpackage.app-version": il launcher jpackage la passa alla
     *     JVM con la riga java-options del .cfg (verificato nel RiPlay.cfg
     *     reale: java-options=-Djpackage.app-version=0.8.4), quindi e' presente
     *     in tutte le installazioni avviate dall'eseguibile
     *  2) file di info di jpackage nella cartella "app" dell'installazione:
     *     - <launcher>.cfg (es. RiPlay.cfg): SEMPRE presente in ogni
     *       installazione (msi/zip/deb/rpm/dmg), quindi cercato PER PRIMO;
     *       si legge la riga java-options=-Djpackage.app-version=..., poi
     *       eventualmente la chiave app.version / app-version, in ultima
     *       battuta il nome del jar principale nella prima riga app.classpath
     *       (es. composeApp-jvm-0.8.4-<hash>.jar -> 0.8.4)
     *     - .jpackage.xml (elemento <app-version>): presente SOLO nelle
     *       distribuzioni zip/app-image (nelle msi non viene incluso),
     *       cercato DOPO il cfg
     *  3) manifest del jar applicativo (Implementation-Version o App-Version)
     *  4) fallback: costante DesktopAppInfo.VERSION (comportamento attuale)
     *
     * La cartella "app" viene cercata con piu' ANCHOR indipendenti (percorsi
     * reali del jar tramite codeSource, jpackage.app-path, java.home del
     * runtime incluso nel pacchetto, cartella di lavoro corrente, eseguibile
     * java del processo e - fondamentale - PROCESSO PADRE: l'eseguibile che ha
     * avviato la JVM, es. RiPlay.exe, la cui cartella contiene le
     * sottocartelle dell'installazione tra cui app/), ciascuno con risalita
     * nelle cartelle superiori e varianti di layout: <root>\app (Windows),
     * <root>/lib/app (Linux), <root>/Contents/app (macOS). In ultima battuta
     * viene scandita la posizione standard di installazione (vedi
     * resolveFromStandardInstallLocations). Ogni sonda viene registrata nella
     * diagnostica per capire immediatamente perche' un file non viene trovato.
     *
     * Tutti i passi sono best-effort e protetti: in IDE / run da Gradle (nessun
     * installer) si cade sul fallback e il comportamento resta identico a
     * prima. Nessuna di queste letture puo' lanciare eccezioni verso l'esterno.
     */
    val localVersionInfo: LocalVersionInfo
        get() = localVersionCache ?: resolveAndCache()

    /**
     * versione locale dinamica: sostituisce DesktopAppInfo.VERSION nel
     * confronto con la remota e puo' essere mostrata direttamente anche in
     * About/Impostazioni (vedi LocalAppVersion in fondo al file).
     */
    val localVersion: String get() = localVersionInfo.version

    /** ri-rileva la versione locale da zero (es. dopo un aggiornamento in-place) */
    fun refreshLocalVersion(): LocalVersionInfo {
        synchronized(this) { localVersionCache = null }
        return localVersionInfo
    }

    @Synchronized
    private fun resolveAndCache(): LocalVersionInfo {
        localVersionCache?.let { return it }
        val resolved = runCatching { resolveLocalVersion() }.getOrElse { e ->
            probe("errore inatteso nella risoluzione: ${e.javaClass.simpleName}: ${e.message}")
            LocalVersionInfo(DesktopAppInfo.VERSION, LocalVersionSource.COMPILE_TIME_FALLBACK)
        }
        localVersionCache = resolved
        return resolved
    }

    private val client: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    /**
     * Scarica e interpreta il file .ver (stesso parsing di Android:
     * split sul "-"). Nessuna eccezione verso l'esterno: fallimento = Failed.
     */
    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val request = HttpRequest.newBuilder(URI.create(VER_URL))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val content = response.body().trim().split("-")

            val remoteCode = content.firstOrNull()?.toIntOrNull() ?: 0
            val remoteName = content.getOrNull(1) ?: ""
            val remoteProduct = content.getOrNull(2) ?: ""

            val local = localVersionInfo
            println("[RiPlay-Update] remota: code=$remoteCode name='$remoteName' product='$remoteProduct' | locale: ${local.describe()}")
            if (local.source == LocalVersionSource.COMPILE_TIME_FALLBACK)
                println("[RiPlay-Update] ATTENZIONE: versione locale non letta dai file di installazione; diagnostica completa in LocalAppVersion.diagnostics() (righe sopra)")

            if (remoteName.isBlank() || remoteCode <= 0) return@runCatching UpdateCheckResult.Failed

            if (isNameNewer(remoteName, local.version))
                UpdateCheckResult.Available(remoteCode, remoteName, remoteProduct)
            else
                UpdateCheckResult.UpToDate
        }.getOrDefault(UpdateCheckResult.Failed)
    }

    /**
     * URL DIRETTO dell'installer per il sistema operativo corrente, costruito
     * dalla versionName letta nel file .ver (STEP 6c). Restituisce null se
     * l'OS non e' tra quelli con asset noto. Su Linux sceglie .deb o .rpm
     * guardando i file distintivi della distribuzione (default .deb).
     */
    fun installerDownloadUrl(versionName: String): String? {
        val v = versionName.trim().removePrefix("v").takeIf { it.isNotBlank() } ?: return null
        val os = System.getProperty("os.name", "").lowercase()
        val asset = when {
            os.contains("win") -> "RiPlay-v$v-Windows.msi"
            os.contains("mac") || os.contains("darwin") -> {
                val arch = System.getProperty("os.arch", "").lowercase()
                if (arch.contains("aarch64") || arch.contains("arm")) "RiPlay-v$v-macOS-arm64.dmg"
                else "RiPlay-v$v-macOS-x86_64.dmg"
            }
            os.contains("nix") || os.contains("nux") || os.contains("linux") -> {
                val isRpm = runCatching {
                    File("/etc/redhat-release").isFile || File("/etc/fedora-release").isFile ||
                            File("/etc/SuSE-release").isFile || File("/etc/centos-release").isFile
                }.getOrDefault(false)
                if (isRpm) "RiPlay-v$v-Linux.rpm" else "RiPlay-v$v-Linux.deb"
            }
            else -> null
        } ?: return null
        return "https://github.com/fast4x/RiPlay/releases/download/v$v/$asset"
    }

    /** TRUE se l'URL risponde 2xx (HEAD con redirect): evita di aprire un link 404 */
    private fun urlExists(url: String): Boolean = runCatching {
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .method("HEAD", HttpRequest.BodyPublishers.noBody())
            .build()
        client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() in 200..299
    }.getOrDefault(false)

    /** apre la pagina releases nel browser di sistema (best-effort) */
    fun openReleasesPage() = openInBrowser(RELEASES_URL)

    fun openInstallerDownload(versionName: String) {
        Thread {
            val direct = runCatching { installerDownloadUrl(versionName) }.getOrNull()
            val url = if (direct != null && urlExists(direct)) direct else RELEASES_URL
            println("[RiPlay-Update] download diretto ($versionName): $url")
            openInBrowser(url)
        }.apply {
            name = "riplay-update-download"
            isDaemon = true
            start()
        }
    }

    /** apertura nel browser di sistema (Desktop API + fallback per OS) */
    fun openInBrowser(url: String) {
        val opened = runCatching {
            val desktop = java.awt.Desktop.getDesktop()
            desktop.isSupported(java.awt.Desktop.Action.BROWSE) &&
                    runCatching { desktop.browse(URI.create(url)) }.isSuccess
        }.getOrDefault(false)
        if (opened) return
        // fallback: comandi nativi (alcuni ambienti Linux non espongono BROWSE)
        runCatching {
            val os = System.getProperty("os.name", "").lowercase()
            val cmd = when {
                os.contains("win") -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
                os.contains("mac") || os.contains("darwin") -> listOf("open", url)
                else -> listOf("xdg-open", url)
            }
            ProcessBuilder(cmd).start()
        }.onFailure { it.printStackTrace() }
    }

    /**
     * confronto semantico "a.b.c" (piu' componenti = ok, non numerici =
     * lessicografico come fallback). TRUE se [remote] e' piu' recente di [local].
     */
    internal fun isNameNewer(remote: String, local: String): Boolean {
        val r = remote.trim().removePrefix("v").split(".")
        val l = local.trim().removePrefix("v").split(".")
        val n = maxOf(r.size, l.size)
        for (i in 0 until n) {
            val rv = r.getOrNull(i)?.toLongOrNull()
            val lv = l.getOrNull(i)?.toLongOrNull()
            when {
                rv != null && lv != null -> {
                    if (rv != lv) return rv > lv
                }
                // un componente numerico vs non numerico: il numerico vince
                rv != null && lv == null -> return true
                rv == null && lv != null -> return false
                else -> {
                    val rs = r.getOrNull(i) ?: ""
                    val ls = l.getOrNull(i) ?: ""
                    if (rs != ls) return rs > ls
                }
            }
        }
        return false
    }

    // ------------------------------------------------------------------
    //  Risoluzione dinamica della versione locale
    // ------------------------------------------------------------------

    private fun resolveLocalVersion(): LocalVersionInfo {
        probes.clear()
        probe("risoluzione versione locale in corso...")

        // 1) proprieta' passata alla JVM dal launcher (java-options del .cfg:
        //    es. java-options=-Djpackage.app-version=0.8.4 in RiPlay.cfg)
        val propVersion = System.getProperty("jpackage.app-version")?.trim()
        if (!propVersion.isNullOrBlank()) {
            probe("proprieta' jpackage.app-version = $propVersion")
            return LocalVersionInfo(propVersion, LocalVersionSource.JPACKAGE_PROPERTY)
        }
        probe("proprieta' jpackage.app-version: assente (avvio non tramite launcher jpackage?)")

        // 2) file di info di jpackage nella cartella "app"
        //    (RiPlay.cfg: sempre presente; .jpackage.xml: solo zip)
        resolveFromJPackageInfoFiles()?.let { return it }

        // 3) manifest del jar applicativo
        resolveFromJarManifest()?.let { return it }

        // 4) scansione delle posizioni standard di installazione (copre gli
        //    avvii FUORI dall'installazione: IDE/Gradle, jar esterno, launcher
        //    senza riferimenti all'installazione)
        if (scanStandardInstallLocations)
            resolveFromStandardInstallLocations()?.let { return it }
        else
            probe("scansione posizioni standard: disattivata")

        // 5) fallback: costante a compile-time + verdetto diagnostico chiaro
        probe("VERDETTO: versione non rilevabile dai file di installazione")
        probe("  - nessun launcher jpackage e nessuna cartella 'app' raggiungibile dagli anchor")
        probe("  - nessun RiPlay.cfg trovato nemmeno nelle posizioni standard di installazione")
        probe("  - l'app sembra girare FUORI da un'installazione (IDE/Gradle/jar esterno)")
        probe("  -> uso la costante di build: ${DesktopAppInfo.VERSION}")
        return LocalVersionInfo(DesktopAppInfo.VERSION, LocalVersionSource.COMPILE_TIME_FALLBACK)
    }

    /**
     * percorso del jar (o della cartella delle classi) da cui e' caricata
     * questa classe, con conversione URL->File robusta: alcuni runtime /
     * classloader producono codeSource con spazi o caratteri speciali NON
     * codificati (%20) che fanno fallire URL.toURI(); in quel caso il
     * percorso viene decodificato manualmente.
     */
    private fun codeSourceFile(): File? = runCatching {
        val location = DesktopUpdateChecker::class.java.protectionDomain?.codeSource?.location
            ?: return@runCatching null
        urlToFile(location)
    }.getOrNull()

    private fun urlToFile(url: URL): File? {
        // URL ben codificato (caso standard)
        runCatching { return File(url.toURI()) }
        // URL mal codificato (spazi/caratteri speciali non in %XX): decodifica manuale
        return runCatching {
            var spec = url.toString()
            if (spec.startsWith("jar:")) spec = spec.substringAfter("jar:").substringBefore("!")
            if (spec.startsWith("file:")) spec = spec.substringAfter("file:")
            File(URLDecoder.decode(spec, "UTF-8"))
        }.getOrNull()
    }

    /**
     * cartelle "app" candidate dell'immagine jpackage: contengono il jar
     * applicativo, il .cfg del launcher (RiPlay.cfg, sempre presente) e il
     * .jpackage.xml (solo zip). Anchor indipendenti, in ordine di affidabilita':
     *   A) percorso reale del jar caricato (codeSource): il jar vive nella
     *      stessa cartella "app" del cfg -> anchor piu' diretto
     *   B) jpackage.app-path (esposto dal launcher jpackage)
     *   C) java.home (runtime incluso nel pacchetto: <root>/runtime oppure
     *      <root>/lib/runtime su Linux)
     *   D) cartella di lavoro corrente (avvio manuale del jar / script)
     *   E) eseguibile java del processo corrente
     * Per ogni anchor si esaminano la cartella stessa, fino a 3 antenati e le
     * varianti di layout delle cartelle "app" dei vari sistemi operativi.
     */
    private fun candidateAppDirs(): Collection<File> {
        val dirs = LinkedHashSet<File>()

        // A) posizione reale del codice dell'app
        codeSourceFile()?.let { cs ->
            probe("anchor codeSource: $cs")
            var dir: File? = cs.parentFile
            repeat(3) {
                val d = dir ?: return@repeat
                addJPackageVariants(dirs, d)
                dir = d.parentFile
            }
        } ?: probe("anchor codeSource: non disponibile")

        // B) percorso del launcher esposto dal launcher jpackage
        System.getProperty("jpackage.app-path")?.trim()?.takeIf { it.isNotBlank() }?.let { path ->
            probe("anchor jpackage.app-path: $path")
            val launcherParent = File(path).parentFile ?: return@let
            addJPackageVariants(dirs, launcherParent)
            launcherParent.parentFile?.let { addJPackageVariants(dirs, it) }
        } ?: probe("anchor jpackage.app-path: assente")

        // C) runtime incluso nel pacchetto (java.home): <root>/runtime su
        //    Windows/macOS, <root>/lib/runtime su Linux deb/rpm
        System.getProperty("java.home")?.trim()?.takeIf { it.isNotBlank() }?.let { home ->
            probe("anchor java.home: $home")
            var dir: File? = File(home)
            repeat(3) {
                dir = dir?.parentFile ?: return@repeat
                addJPackageVariants(dirs, dir!!)
            }
        } ?: probe("anchor java.home: assente")

        // D) cartella di lavoro corrente
        System.getProperty("user.dir")?.trim()?.takeIf { it.isNotBlank() }?.let { wd ->
            probe("anchor user.dir: $wd")
            addJPackageVariants(dirs, File(wd))
            File(wd).parentFile?.let { addJPackageVariants(dirs, it) }
        } ?: probe("anchor user.dir: assente")

        // E) eseguibile java che sta girando (indipendente dalle proprieta')
        runCatching {
            ProcessHandle.current().info().command().orElse(null)?.trim()?.takeIf { it.isNotBlank() }
        }.getOrNull()?.let { cmd ->
            probe("anchor processo: $cmd")
            var dir: File? = File(cmd)
            repeat(4) {
                dir = dir?.parentFile ?: return@repeat
                addJPackageVariants(dirs, dir!!)
            }
        }

        // F) PROCESSO PADRE: l'eseguibile che ha avviato la JVM. Quando l'app
        //    viene lanciata dall'installazione (RiPlay.exe o qualunque launcher)
        //    la JVM e' un processo FIGLIO di quell'eseguibile: dal percorso del
        //    padre si risale alla cartella dell'eseguibile e alle sue
        //    sottocartelle (app/ inclusa) ANCHE quando tutti gli altri anchor
        //    (codeSource, java.home, user.dir, ...) puntano a cartelle esterne
        //    all'installazione.
        val parentCmd = runCatching {
            // nota API: parent() -> Optional<ProcessHandle>, info() -> Info diretto,
            // Info.command() -> Optional<String>
            ProcessHandle.current().parent().orElse(null)?.info()?.command()?.orElse(null)
                ?.trim()?.takeIf { it.isNotBlank() }
        }.getOrNull()
        if (parentCmd != null) {
            probe("anchor processo padre: $parentCmd")
            val launcher = File(parentCmd)
            launcher.parentFile?.let { addJPackageVariants(dirs, it) }
            launcher.parentFile?.parentFile?.let { addJPackageVariants(dirs, it) }
        } else {
            probe("anchor processo padre: non disponibile")
        }

        return dirs
    }

    /** aggiunge [dir] e le varianti standard delle cartelle "app" di jpackage */
    private fun addJPackageVariants(dirs: MutableCollection<File>, dir: File) {
        if (!dir.isDirectory) return
        dirs.add(dir)
        dirs.add(File(dir, "app"))            // Windows: <root>\app
        dirs.add(File(dir, "lib/app"))        // Linux:   <root>/lib/app
        dirs.add(File(dir, "Contents/app"))   // macOS:   <root>/Contents/app
    }

    private fun resolveFromJPackageInfoFiles(): LocalVersionInfo? {
        val examined = ArrayList<String>()
        for (dir in candidateAppDirs()) {
            if (!dir.isDirectory) continue

            // RiPlay.cfg (o <launcher>.cfg): presente in TUTTE le installazioni
            // jpackage (msi, zip, deb, rpm, dmg) -> cercato PER PRIMO
            val cfgFiles = dir.listFiles { f -> f.isFile && f.name.lowercase().endsWith(".cfg") }.orEmpty()
            for (cfg in cfgFiles) {
                probe("cartella app '$dir': trovato ${cfg.name}")
                parseCfgAppVersion(cfg)?.takeIf { it.isNotBlank() }?.let {
                    probe("versione locale letta da ${cfg.name}: $it")
                    return LocalVersionInfo(it, LocalVersionSource.JPACKAGE_INFO_FILE, cfg)
                }
            }

            // .jpackage.xml: presente solo nelle distribuzioni zip/app-image
            val xml = File(dir, ".jpackage.xml")
            if (xml.isFile) {
                parseXmlAppVersion(xml)?.takeIf { it.isNotBlank() }?.let {
                    probe("versione locale letta da ${xml.name}: $it")
                    return LocalVersionInfo(it, LocalVersionSource.JPACKAGE_INFO_FILE, xml)
                }
            }

            examined += dir.path
        }
        if (examined.isEmpty())
            probe("nessuna cartella 'app' di jpackage trovata con gli anchor disponibili")
        else
            probe("nessuna versione leggibile nei file info jpackage (cartelle esaminate: $examined)")

        // spiegazione immediata nel log quando il jar caricato e' esterno
        // all'installazione (caso "cartelle risolte tutte esterne")
        val cs = codeSourceFile()
        if (cs != null) {
            val insideApp = generateSequence(cs.parentFile) { it.parentFile }.take(4)
                .any { it.name.equals("app", ignoreCase = true) }
            if (insideApp)
                probe("il jar caricato e' dentro una cartella 'app' ma senza cfg/xml con versione leggibile")
            else
                probe("il jar caricato NON e' dentro una cartella 'app' di installazione: ${cs.path}")
        }
        return null
    }

    /**
     * ultima spiaggia prima del fallback: cerca RiPlay.cfg / .jpackage.xml
     * nelle posizioni STANDARD di installazione, esaminando SOLO le cartelle
     * il cui nome inizia per "riplay" (per non leggere cfg di altri programmi).
     *
     * Serve quando l'app gira FUORI dall'installazione (avvio da IDE/Gradle,
     * jar esterno, launcher che non espone alcun riferimento all'installazione)
     * e nessun anchor punta alla cartella dell'eseguibile: in quel caso viene
     * comunque letta la versione dell'app INSTALLATA nel sistema.
     *
     * Radici esaminate per OS (oltre alle cartelle personalizzate indicate
     * nella variabile d'ambiente RIPLAY_INSTALL_DIRS, separate da
     * File.pathSeparator):
     *   Windows: %ProgramFiles%, %ProgramFiles(x86)%, %ProgramW6432%,
     *            %LOCALAPPDATA%, %LOCALAPPDATA%\\Programs, C:\\Program Files
     *   macOS:   /Applications
     *   Linux:   /opt, /usr/lib
     * Disattivabile impostando DesktopUpdateChecker.scanStandardInstallLocations = false.
     */
    var scanStandardInstallLocations: Boolean = true

    private fun resolveFromStandardInstallLocations(): LocalVersionInfo? {
        val os = System.getProperty("os.name", "").lowercase()
        val roots = LinkedHashSet<File>()

        // radici personalizzate (utile per installazioni in cartelle non standard)
        System.getenv("RIPLAY_INSTALL_DIRS")?.split(File.pathSeparator)
            ?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?.forEach { roots.add(File(it)) }

        if (os.contains("win")) {
            sequenceOf(
                System.getenv("ProgramFiles"),
                System.getenv("ProgramFiles(x86)"),
                System.getenv("ProgramW6432"),
                System.getenv("LOCALAPPDATA"),
                System.getenv("LOCALAPPDATA")?.let { "$it\\Programs" },
                "C:\\Program Files"
            ).filterNotNull().forEach { roots.add(File(it)) }
        } else if (os.contains("mac") || os.contains("darwin")) {
            roots.add(File("/Applications"))
        } else {
            roots.add(File("/opt"))
            roots.add(File("/usr/lib"))
        }

        for (root in roots) {
            if (!root.isDirectory) continue
            val apps = root.listFiles { d, name ->
                File(d, name).isDirectory && name.lowercase().startsWith("riplay")
            }.orEmpty()
            for (appRoot in apps) {
                val variants = listOf(
                    appRoot,
                    File(appRoot, "app"),          // Windows: <root>\\app
                    File(appRoot, "lib/app"),      // Linux:   <root>/lib/app
                    File(appRoot, "Contents/app")  // macOS:   <root>/Contents/app
                )
                for (appDir in variants) {
                    if (!appDir.isDirectory) continue
                    probe("scansione posizioni standard: '$appDir'")

                    val cfgFiles = appDir.listFiles { f -> f.isFile && f.name.lowercase().endsWith(".cfg") }.orEmpty()
                    for (cfg in cfgFiles) {
                        parseCfgAppVersion(cfg)?.takeIf { it.isNotBlank() }?.let {
                            probe("versione locale letta da ${cfg.absolutePath} (posizioni standard)")
                            return LocalVersionInfo(it, LocalVersionSource.JPACKAGE_INFO_FILE, cfg)
                        }
                    }

                    val xml = File(appDir, ".jpackage.xml")
                    if (xml.isFile) {
                        parseXmlAppVersion(xml)?.takeIf { it.isNotBlank() }?.let {
                            probe("versione locale letta da ${xml.absolutePath} (posizioni standard)")
                            return LocalVersionInfo(it, LocalVersionSource.JPACKAGE_INFO_FILE, xml)
                        }
                    }
                }
            }
        }
        probe("scansione posizioni standard: nessun RiPlay.cfg trovato")
        return null
    }

    /** legge l'elemento <app-version> dal .jpackage.xml generato da jpackage (JDK 17+) */
    private fun parseXmlAppVersion(xml: File): String? = runCatching {
        Regex("<app-version\\s*>([^<]+)</app-version>")
            .find(xml.readText())?.groupValues?.get(1)?.trim()
    }.getOrNull()

    /**
     * riga java-options che definisce la proprieta' jpackage.app-version
     * (es. java-options=-Djpackage.app-version=0.8.4 nel RiPlay.cfg reale di
     * Compose Desktop: e' la riga da cui il launcher ricava la proprieta')
     */
    private val CFG_JPACKAGE_APP_VERSION =
        Regex("^-Djpackage\\.app-version=(.+)$", RegexOption.IGNORE_CASE)

    /**
     * legge la versione locale dal .cfg del launcher jpackage (es. RiPlay.cfg),
     * su tre livelli in ordine di affidabilita':
     *  a) chiave app.version / app-version (formato storico di jpackage)
     *  b) riga java-options=-Djpackage.app-version=... (formato attuale di
     *     Compose Desktop/jpackage, verificato nel RiPlay.cfg reale)
     *  c) ultima spiaggia: nome del jar principale nella PRIMA riga
     *     app.classpath (es. composeApp-jvm-0.8.4-<hash>.jar -> 0.8.4)
     */
    private fun parseCfgAppVersion(cfg: File): String? = runCatching {
        var fromMainJar: String? = null
        cfg.useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                val sep = line.indexOf('=')
                if (sep <= 0) continue
                val key = line.substring(0, sep).trim().lowercase()
                val value = line.substring(sep + 1).trim()
                when (key) {
                    // a) formato storico: app.version=1.2.3
                    "app.version", "app-version" ->
                        value.takeIf { it.isNotBlank() }?.let { return@useLines stripQuotes(it) }

                    // b) formato attuale compose/jpackage: java-options=-Djpackage.app-version=0.8.4
                    "java-options" ->
                        CFG_JPACKAGE_APP_VERSION.find(value)?.groupValues?.get(1)
                            ?.trim()?.takeIf { it.isNotBlank() }?.let { return@useLines stripQuotes(it) }

                    // c) candidato dal nome del jar principale (prima riga app.classpath)
                    "app.classpath" ->
                        if (fromMainJar == null) fromMainJar = versionFromMainJarName(value)
                }
            }
            fromMainJar
        }
    }.getOrNull()

    /** rimuove un eventuale quoting dal valore (es. "0.8.4") */
    private fun stripQuotes(value: String): String =
        value.trim().removeSurrounding("\"").removeSurrounding("'").trim()

    /**
     * ricava la versione dal nome del jar principale di Compose Desktop,
     * es. "composeApp-jvm-0.8.4-7a4e78a1942ce419be7eb16ca531166.jar" -> "0.8.4"
     * (il jar principale e' la prima riga app.classpath del .cfg)
     */
    private fun versionFromMainJarName(classpathValue: String): String? {
        val fileName = classpathValue.substringAfterLast('\\').substringAfterLast('/')
        return Regex("-(\\d+(?:\\.\\d+)+)-[0-9a-fA-F]{8,}\\.jar$")
            .find(fileName)?.groupValues?.get(1)
    }

    /**
     * manifest del jar che contiene questa classe: funziona anche per il fat
     * jar se la build Gradle imposta Implementation-Version (o App-Version).
     */
    private fun resolveFromJarManifest(): LocalVersionInfo? = runCatching {
        val jar = codeSourceFile() ?: return@runCatching null
        if (!jar.isFile || !jar.name.lowercase().endsWith(".jar")) return@runCatching null
        JarFile(jar).use { jf ->
            val attrs = jf.manifest?.mainAttributes ?: return@runCatching null
            val v = (attrs.getValue("Implementation-Version") ?: attrs.getValue("App-Version"))
                ?.trim()?.takeIf { it.isNotBlank() }
            v?.let { LocalVersionInfo(it, LocalVersionSource.JAR_MANIFEST, jar) }
        }
    }.getOrNull()
}


/**
 * API pubblica e riutilizzabile per la versione locale rilevata a runtime
 * dall'eseguibile/installazione jpackage: pensata per essere richiamata da
 * qualunque punto dell'app desktop, INCLUSE le schermate Impostazioni/About.
 *
 * Esempi d'uso (in Impostazioni/About):
 *   LocalAppVersion.version()            -> "0.8.4"
 *   LocalAppVersion.describe()           -> "0.8.4 (file info jpackage ...: RiPlay.cfg)"
 *   LocalAppVersion.isFromInstallFiles() -> true se letta da cfg/xml/manifest,
 *                                           false se e' il fallback di build
 *   LocalAppVersion.diagnostics()        -> sonde effettuate (per debug/supporto)
 *   LocalAppVersion.refresh()            -> ri-rileva da zero
 */
object LocalAppVersion {

    /** versione locale + sorgente (cache: risolta al primo accesso) */
    fun info(): DesktopUpdateChecker.LocalVersionInfo = DesktopUpdateChecker.localVersionInfo

    /** solo la versione locale rilevata (es. "0.8.4") */
    fun version(): String = info().version

    /** versione + da dove e' stata letta, pronta per la UI */
    fun describe(): String = info().describe()

    /** da dove e' stata letta la versione */
    fun source(): DesktopUpdateChecker.LocalVersionSource = info().source

    /** file dal quale e' stata letta la versione (null se proprieta'/fallback) */
    fun sourceFile(): File? = info().file

    /** TRUE se la versione viene dai file di installazione, FALSE se fallback */
    fun isFromInstallFiles(): Boolean =
        source() != DesktopUpdateChecker.LocalVersionSource.COMPILE_TIME_FALLBACK

    /** sonde effettuate durante il rilevamento (per debug/supporto in Impostazioni) */
    fun diagnostics(): List<String> = DesktopUpdateChecker.diagnostics()

    /** ri-rileva la versione locale da zero */
    fun refresh(): DesktopUpdateChecker.LocalVersionInfo = DesktopUpdateChecker.refreshLocalVersion()
}

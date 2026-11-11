# desktop-resources — risorse incluse nei pacchetti desktop (deb/msi/dmg)

Questa cartella viene copiata DENTRO l'applicazione installata dalla DSL di
Compose Desktop (`appResourcesRootDir`, sottocartelle per sistema:
`common | linux | windows | macos`) e alla fine finisce in
`<app>/resources` nel pacchetto installato.

## CEF pre-seminato (opzionale ma consigliato) — FORMATO v46 (jcefmaven)

Dalla v46 il motore browser e' **jcefmaven** (me.friwi:jcefmaven, Chromium
corrente) al posto di KCEF (dev.datlag:kcef, non piu' aggiornato). Per
costruire pacchetti SENZA il download di ~150MB al primo avvio dell'utente,
semina qui l'archivio natives di jcefmaven PRIMA della build:

- Windows:  `desktop-resources/windows/kcef/jcef-natives-windows-amd64-<tag>.tar.gz`
- Linux:    `desktop-resources/linux/kcef/jcef-natives-linux-amd64-<tag>.tar.gz`
- macOS:    `desktop-resources/macos/kcef/jcef-natives-macosx-amd64-<tag>.tar.gz`
            (Intel) oppure `jcef-natives-macosx-arm64-<tag>.tar.gz` (Apple Silicon)

Dove ottenere l'archivio (il `<tag>` e' il release tag di jcefmaven, es.
`jcef-17e805a+cef-152.0.6+g708dc14+chromium-152.0.7977.83`):

1. GitHub release di jcefmaven: https://github.com/jcefmaven/jcefmaven/releases
   → asset `jcef-natives-<platform>-<tag>.jar` → il .jar CONTIENE al suo
   interno `jcef-natives-<platform>-<tag>.tar.gz`: estrarlo
   (`unzip jcef-natives-<platform>-<tag>.jar jcef-natives-<platform>-<tag>.tar.gz`);
2. Maven Central: https://repo.maven.apache.org/maven2/me/friwi/jcef-natives-<platform>/
   (versione = release tag) → stesso jar, stesso procedimento.

IMPORTANTISSIMO: il tag dell'archivio deve corrispondere ALLA VERSIONE di
jcefmaven usata dalla build (gradle/libs.versions.toml): un archivio con tag
diverso viene ignorato dall'app e jcefmaven scarichera' comunque il bundle
corretto (il check e' `CefBuildInfo.fromClasspath()` vs nome file).

Il VECCHIO formato `jbr_jcef-*.tar.gz` (era KCEF/JetBrainsRuntime) NON e'
piu' compatibile: l'app lo segnala come ignorato nei log.

Poi costruisci il pacchetto:

    ./gradlew packageMsi            (Windows)
    ./gradlew packageDeb packageRpm (Linux)
    ./gradlew packageDmg            (macOS)

L'archivio finisce in `resources/kcef/` dentro il pacchetto e l'app, al primo
avvio, lo estrae nella directory dati utente (`install.lock` + `build_meta.json`
creati automaticamente): NESSUN download per l'utente.

Se NON semini nulla, l'app si comporta come sempre: jcefmaven scarica il bundle
natives (GitHub release di jcefmaven -> Maven Central) al primo avvio
(fallback automatico).

Il workflow GitHub Actions unificato `.github/workflows/build-release.yml`
(Android full/foss + desktop MSI/DEB/RPM/DMG in un'unica release) esegue la
semina automaticamente per ogni piattaforma.

Nota: gli archivi .tar.gz NON vanno committati nella repository (peso ~150MB):
li scarica lo script o il workflow.

Nota 2 (v46): la cache CEF (cookie/consenso/login) vive FUORI dalla directory
del bundle, in `<dati>/kcef-cache`: qualunque reinstallazione o aggiornamento
del bundle NON fa piu' perdere l'accesso agli account.

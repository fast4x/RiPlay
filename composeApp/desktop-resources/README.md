# desktop-resources — risorse incluse nei pacchetti desktop (deb/msi/dmg)

Questa cartella viene copiata DENTRO l'applicazione installata dalla DSL di
Compose Desktop (`appResourcesRootDir`, sottocartelle per sistema:
`common | linux | windows | macos`) e alla fine finisce in
`<app>/resources` nel pacchetto installato.

## CEF pre-seminato (opzionale ma consigliato)

Per costruire pacchetti SENZA il download di ~200MB al primo avvio dell'utente,
semina qui l'archivio CEF (jbr_jcef) PRIMA della build:

- Windows:  `fetchKcefBundle.cmd`  (nella radice del progetto)
  → scarica `desktop-resources/windows/kcef/jbr_jcef-<ver>-windows-x64-<b>.tar.gz`
- Linux:    `bash scripts/fetch-kcef-bundle.sh`
  → scarica `desktop-resources/linux/kcef/jbr_jcef-<ver>-linux-x64-<b>.tar.gz`
- macOS:    `bash scripts/fetch-kcef-bundle.sh`
  → scarica `desktop-resources/macos/kcef/jbr_jcef-<ver>-osx-<arch>-<b>.tar.gz`

Lo script seleziona l'archivio con la STESSA logica della libreria KCEF
(release "latest" di JetBrains/JetBrainsRuntime). Poi costruisci il pacchetto:

    ./gradlew packageMsi            (Windows)
    ./gradlew packageDeb packageRpm (Linux)
    ./gradlew packageDmg            (macOS)

L'archivio finisce in `resources/kcef/` dentro il pacchetto e l'app, al primo
avvio, lo estrae nella directory dati utente: NESSUN download per l'utente.

Se NON semini nulla, l'app si comporta come sempre: scarica il bundle CEF da
GitHub al primo avvio (fallback automatico).

Il workflow GitHub Actions unificato `.github/workflows/build-release.yml`
(Android full/foss + desktop MSI/DEB/RPM/DMG in un'unica release) esegue la
semina automaticamente per ogni piattaforma.

Nota: gli archivi .tar.gz NON vanno committati nella repository (peso ~250MB):
li scarica lo script o il workflow.

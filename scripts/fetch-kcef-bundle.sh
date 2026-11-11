#!/usr/bin/env bash
# =============================================================================
# fetch-kcef-bundle.sh — SEMINA CEF PRE-PACCHETTO (FORMATO v46 / jcefmaven)
#
# Chiamato dal workflow build-release.yml (step "Pre-seed bundle CEF") con la
# STESSA interfaccia CLI della versione precedente:
#
#     fetch-kcef-bundle.sh <seed_os> <seed_arch>
#       seed_os:   windows | linux | osx
#       seed_arch: x64 | aarch64
#
# v46 (migrazione KCEF -> jcefmaven): l'app non riconosce piu' gli archivi
# jbr_jcef (JetBrains Runtime) usati dall'era KCEF — KcefEngine.packagedCefBundle()
# accetta SOLO "jcef-natives-<platform>-<tag>.tar.gz", lo stesso archivio che
# jcefmaven estrae al primo avvio (v. composeApp/desktop-resources/README.md).
# Questo script scarica esattamente QUELLO archivio per la piattaforma del job
# e lo semina in composeApp/desktop-resources/<os>/kcef/ cosi' jpackage lo
# include nell'installer (finisce in <app>/resources/kcef/): l'utente finale
# NON scarica ~142MB al primo avvio.
#
# PIPELINE:
#   1) versione jcefmaven letta da gradle/libs.versions.toml (fonte di verita'
#      della build: il tag dell'archivio DEVE coincidere con CefBuildInfo
#      dell'app, altrimenti il pre-seed viene ignorato dall'app stessa);
#   2) mapping seed_os/seed_arch -> platform jcefmaven (EnumPlatform):
#        windows+x64     -> windows-amd64  (cartella windows)
#        linux+x64       -> linux-amd64    (cartella linux)
#        linux+arm64     -> linux-arm64    (cartella linux, futuro)
#        osx+aarch64     -> macosx-arm64   (cartella macos)
#        osx+x64         -> macosx-amd64   (cartella macos)
#      NB: la cartella macOS e' "macos" (sottocartella riconosciuta dalla DSL
#      Compose appResourcesRootDir: common|linux|windows|macos), NON "osx";
#   3) TAG dell'archivio = release tag completo (es.
#      "jcef-17e805a+cef-152.0.6+g708dc14+chromium-152.0.7977.83"), letto dagli
#      asset della release GitHub di jcefmaven (API, versione esatta del toml);
#      fallback: maven-metadata.xml su Maven Central (match su cef-<version>);
#   4) DOWNLOAD del jar natives (che CONTIENE il .tar.gz):
#      primario  = release asset GitHub (come fa jcefmaven stesso, v.
#                  CefAppBuilder: .../releases/download/<mvn_version>/jcef-natives-<platform>-<tag>.jar)
#      fallback  = Maven Central (me/friwi/jcef-natives-<platform>/<tag>/...)
#   5) estrazione del .tar.gz dal jar: python (zipfile) -> unzip -> jar (JDK);
#   6) canarini: dimensione >= 50MB, "tar -tzf" leggibile, build_meta.json
#      presente ALLA RADICE dell'archivio (richiesto dal pre-seed dell'app:
#      rende l'installazione completa senza ri-download).
#
# Exit codes: 1 = input/env invalidi, 2 = tag non trovato, 3 = download
# fallito, 4 = estrazione fallita, 5 = canarino fallito.
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SEED_OS="${1:-}"
SEED_ARCH="${2:-}"

log()  { echo "[fetch-jcef-natives] $*"; }
fail() { echo "[fetch-jcef-natives] ERRORE: $*" >&2; exit 1; }

# ---------------------------------------------------------------------------
# 1) Versione jcefmaven dal toml (es. "152.0.6")
# ---------------------------------------------------------------------------
TOML="$REPO_ROOT/gradle/libs.versions.toml"
[ -f "$TOML" ] || fail "gradle/libs.versions.toml non trovato in $REPO_ROOT (eseguire dalla root repo)"
JV="$(grep -E '^jcefmaven[[:space:]]*=' "$TOML" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)"
[ -n "$JV" ] || fail "versione jcefmaven non trovata in gradle/libs.versions.toml (chiave 'jcefmaven = \"...\"')"
log "versione jcefmaven (gradle/libs.versions.toml): $JV"

# ---------------------------------------------------------------------------
# 2) Mapping seed_os/seed_arch -> platform jcefmaven + sottocartella resources
# ---------------------------------------------------------------------------
case "${SEED_OS}" in
  windows)
    PLATFORM="windows-amd64"; OS_DIR="windows"
    ;;
  linux)
    case "${SEED_ARCH}" in
      x64|amd64|x86_64)    PLATFORM="linux-amd64"  ;;
      aarch64|arm64)       PLATFORM="linux-arm64"  ;;
      *) fail "seed_arch non supportato per linux: '$SEED_ARCH' (attesi x64|aarch64)" ;;
    esac
    OS_DIR="linux"
    ;;
  osx|macos)
    case "${SEED_ARCH}" in
      aarch64|arm64)       PLATFORM="macosx-arm64" ;;
      x64|amd64|x86_64)    PLATFORM="macosx-amd64" ;;
      *) fail "seed_arch non supportato per osx: '$SEED_ARCH' (attesi x64|aarch64)" ;;
    esac
    OS_DIR="macos"
    ;;
  *)
    fail "seed_os non supportato: '$SEED_OS' (attesi windows|linux|osx)"
    ;;
esac
log "platform jcefmaven: $PLATFORM  ->  desktop-resources/$OS_DIR/kcef/"

DEST_DIR="$REPO_ROOT/composeApp/desktop-resources/$OS_DIR/kcef"
mkdir -p "$DEST_DIR"

# ---------------------------------------------------------------------------
# 3) TAG completo: release GitHub di jcefmaven (versione esatta del toml),
#    asset "jcef-natives-<platform>-<tag>.jar". Fallback: maven-metadata.xml.
# ---------------------------------------------------------------------------
FETCH_TAG_URL="https://api.github.com/repos/jcefmaven/jcefmaven/releases/tags/$JV"
CURL_AUTH=()
if [ -n "${GITHUB_TOKEN:-}" ]; then
  CURL_AUTH=(-H "Authorization: Bearer $GITHUB_TOKEN")
  log "GitHub API con token (rate limit elevato)"
else
  log "GitHub API anonima (rate limit 60/h per IP)"
fi

TAG=""
REL_JSON="$(curl -fsSL "${CURL_AUTH[@]}" "$FETCH_TAG_URL" 2>/dev/null || true)"
if [ -n "$REL_JSON" ]; then
  TAG="$(printf '%s' "$REL_JSON" \
    | grep -oE '"name"[[:space:]]*:[[:space:]]*"jcef-natives-'"$PLATFORM"'-[^"]+\.jar"' \
    | head -1 \
    | sed -E 's/^"name"[[:space:]]*:[[:space:]]*"jcef-natives-'"$PLATFORM"'-//; s/\.jar"$/''/')"
fi

if [ -z "$TAG" ]; then
  log "asset non trovato via GitHub API: fallback maven-metadata.xml (Maven Central)"
  CEF_ESC="$(printf '%s' "$JV" | sed -E 's/\./\\./g')"
  # Maven Central puo' rispondere 429 (rate limit per IP): retry espliciti
  # con pausa (curl --retry non copre sempre i 429 su tutte le versioni).
  META=""
  for attempt in 1 2 3 4; do
    META="$(curl -fsSL "https://repo.maven.apache.org/maven2/me/friwi/jcef-natives-$PLATFORM/maven-metadata.xml" 2>/dev/null || true)"
    if [ -n "$META" ]; then break; fi
    log "maven-metadata.xml non disponibile (tentativo $attempt/4, possibile rate limit): attendo 5s"
    sleep 5
  done
  TAG="$(printf '%s' "$META" \
    | grep -oE '<version>jcef-[0-9a-f]+\+cef-'"$CEF_ESC"'\+g[^<]*</version>' \
    | tail -1 \
    | sed -E 's|</?version>||g' || true)"
fi
[ -n "$TAG" ] || fail "impossibile determinare il release tag per platform=$PLATFORM e jcefmaven=$JV (exit 2)"
log "release tag dell'archivio: $TAG"

# ---------------------------------------------------------------------------
# Sanity: l'app IGNORA un archivio con tag diverso dalla sua CefBuildInfo
# (preSeedPackagedBundle -> expectedBundleTag). Con il tag preso dalla release
# della STESSA versione del toml la corrispondenza e' garantita per costruzione;
# qui si verifica anche che il tag citi la versione cef attesa.
# ---------------------------------------------------------------------------
case "$TAG" in
  *"cef-$JV"*|*"cef-$JV+"*) : ;;   # es. cef-152.0.6+...
  *) fail "il tag '$TAG' non cita la versione cef-$JV attesa dall'app: l'archivio verrebbe ignorato dal pre-seed (exit 2)" ;;
esac

TARGZ_NAME="jcef-natives-$PLATFORM-$TAG.tar.gz"
DEST_FILE="$DEST_DIR/$TARGZ_NAME"

# ---------------------------------------------------------------------------
# Idempotenza: archivio gia' seminato (stesso nome) e plausibile -> skip
# ---------------------------------------------------------------------------
if [ -f "$DEST_FILE" ]; then
  EXISTING_SIZE="$(wc -c < "$DEST_FILE" 2>/dev/null || echo 0)"
  if [ "$EXISTING_SIZE" -ge 52428800 ]; then
    log "archivio gia' presente e valido ($EXISTING_SIZE byte): skip download"
    exit 0
  fi
  log "archivio presente ma sospetto ($EXISTING_SIZE byte): re-download"
  rm -f "$DEST_FILE"
fi

# ---------------------------------------------------------------------------
# 4) Download del jar natives (CONTIENE il .tar.gz come unico grande entry)
# ---------------------------------------------------------------------------
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
JAR_FILE="$WORK/jcef-natives-$PLATFORM-$TAG.jar"

GH_URL="https://github.com/jcefmaven/jcefmaven/releases/download/$JV/jcef-natives-$PLATFORM-$TAG.jar"
MC_URL="https://repo.maven.apache.org/maven2/me/friwi/jcef-natives-$PLATFORM/$TAG/jcef-natives-$PLATFORM-$TAG.jar"

log "download jar (primario GitHub): $GH_URL"
if ! curl -fL --retry 3 --retry-delay 5 --max-time 900 -o "$JAR_FILE" "$GH_URL"; then
  log "download GitHub fallito: fallback Maven Central"
  curl -fL --retry 3 --retry-delay 5 --max-time 900 -o "$JAR_FILE" "$MC_URL" \
    || fail "download natives fallito da GitHub e Maven Central (exit 3)"
fi
JAR_SIZE="$(wc -c < "$JAR_FILE")"
log "jar scaricato: $JAR_SIZE byte"
[ "$JAR_SIZE" -ge 52428800 ] || fail "jar sospettosamente piccolo ($JAR_SIZE byte): download corrotto? (exit 3)"

# ---------------------------------------------------------------------------
# 5) Estrazione del .tar.gz dal jar: python (zipfile) -> unzip -> jar (JDK)
# ---------------------------------------------------------------------------
extract_via_python() {
  local PY=""
  if command -v python3 >/dev/null 2>&1; then PY=python3
  elif command -v python >/dev/null 2>&1; then PY=python
  else return 1; fi
  "$PY" - "$JAR_FILE" "$TARGZ_NAME" "$DEST_FILE" <<'PYEOF' || return 1
import sys, zipfile
jar_path, entry, out_path = sys.argv[1], sys.argv[2], sys.argv[3]
with zipfile.ZipFile(jar_path) as z:
    with z.open(entry) as src, open(out_path, "wb") as dst:
        while True:
            chunk = src.read(4 * 1024 * 1024)
            if not chunk:
                break
            dst.write(chunk)
PYEOF
}
extract_via_unzip() {
  command -v unzip >/dev/null 2>&1 || return 1
  unzip -j -o -q "$JAR_FILE" "$TARGZ_NAME" -d "$DEST_DIR" >/dev/null 2>&1
  [ -f "$DEST_FILE" ]
}
extract_via_jar() {
  command -v jar >/dev/null 2>&1 || return 1
  mkdir -p "$WORK/extract"
  (cd "$WORK/extract" && jar xf "$JAR_FILE" "$TARGZ_NAME") || return 1
  [ -f "$WORK/extract/$TARGZ_NAME" ] || return 1
  mv -f "$WORK/extract/$TARGZ_NAME" "$DEST_FILE"
}

log "estrazione di $TARGZ_NAME dal jar"
if extract_via_python; then
  log "estrazione via python: OK"
elif extract_via_unzip; then
  log "estrazione via unzip: OK"
elif extract_via_jar; then
  log "estrazione via jar (JDK): OK"
else
  fail "impossibile estrarre $TARGZ_NAME dal jar (python/unzip/jar tutti assenti o falliti) (exit 4)"
fi

# ---------------------------------------------------------------------------
# 6) Canarini sull'archivio seminato
# ---------------------------------------------------------------------------
TGZ_SIZE="$(wc -c < "$DEST_FILE")"
log "archivio seminato: $DEST_FILE ($TGZ_SIZE byte)"
if [ "$TGZ_SIZE" -lt 52428800 ]; then
  fail "archivio seminato sospettosamente piccolo ($TGZ_SIZE byte, attesi >= 50MB) (exit 5)"
fi
if ! tar -tzf "$DEST_FILE" >/dev/null 2>&1; then
  fail "tar -tzf non riesce a leggere l'archivio: file corrotto (exit 5)"
fi
# NB: l'elenco va letto IN UNA VARIABILE: "tar -tzf | grep -q" con
# set -o pipefail e' soggetto a SIGPIPE su tar (grep esce al match, tar
# riceve SIGPIPE -> pipeline falsata). Su macosx-arm64 e linux-amd64 la
# radice dell'archivio e' piatta (entry 'build_meta.json' letterale).
TAR_ENTRIES="$(tar -tzf "$DEST_FILE")"
if ! printf '%s\n' "$TAR_ENTRIES" | grep -qE '^\.?/?build_meta\.json$'; then
  fail "build_meta.json NON presente alla radice dell'archivio: il pre-seed dell'app non riuscirebbe (exit 5)"
fi

log "OK: CEF pre-seminato per $PLATFORM in desktop-resources/$OS_DIR/kcef/"
log "     l'installer includera' $TARGZ_NAME (nessun download per l'utente al primo avvio)"

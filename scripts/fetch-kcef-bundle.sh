#!/usr/bin/env bash
# =============================================================================
# fetch-kcef-bundle.sh - Pre-seed bundle CEF (jcefmaven natives) nel repo
#
# CLI (invariata dal STEP 3ae):
#   bash scripts/fetch-kcef-bundle.sh <seed_os> <seed_arch>
#     seed_os   : windows | linux | osx
#     seed_arch : x64 | aarch64
#   Da eseguire dalla RADICE del repo (usa gradle/libs.versions.toml e
#   composeApp/desktop-resources/<os>/kcef/). GITHUB_TOKEN opzionale ma
#   consigliato in CI (rate limit API elevato).
#
# COSA FA:
#   1. legge la versione jcefmaven da gradle/libs.versions.toml (fonte di
#      verita' della build);
#   2. risolve il tag COMPLETO del bundle natives (es.
#      jcef-17e805a+cef-152.0.6+g708dc14+chromium-152.0.7977.83) con TRE
#      strade in cascata:
#        (a) GitHub API: asset della release <versione> di jcefmaven,
#            con GITHUB_TOKEN;
#        (b) asset "build_meta.json" della release (2KB): campo release_tag
#            (non richiede API: nessun rate limit);
#        (c) Maven Central: maven-metadata.xml di me.friwi:jcef-natives-
#            <platform> (le stesse coordinate che usa CefAppBuilder);
#   3. scarica il jar natives (primario GitHub release asset, fallback
#      Maven Central) e ne verifica canarini (taglia, magic zip, sha256
#      quando l'API lo dichiara);
#   4. estrae il .tar.gz dal jar (python -> unzip -> jar JDK: copre tutti
#      i runner);
#   5. semina in composeApp/desktop-resources/<windows|linux|macos>/kcef/
#      come "jcef-natives-<platform>-<tag>.tar.gz" (formato accettato da
#      KcefEngine.packagedCefBundle()) con canarini: dimensione >= 50MB,
#      tar leggibile, build_meta.json alla radice dell'archivio (richiesto
#      dal pre-seed dell'app: install.lock + meta senza ri-download).
#
# STEP 3ai (fix tag -javadoc, nov 2026):
#   SINTOMO CI: "ERRORE: jar sospettosamente piccolo (76279 byte)" (exit 3)
#   su TUTTI i job desktop. CAUSA: la release jcefmaven 152.0.6 pubblica per
#   OGNI piattaforma TRE jar con lo stesso prefisso: il natives vero (es.
#   windows-amd64 = 163MB), la variante "-javadoc.jar" (74.5KB) e la
#   variante "-sources.jar" (~3KB). La risoluzione del tag dagli asset
#   matchava anche le varianti e - preso il primo match (in ordine
#   alfabetico "-javadoc" precede il jar puro) - scaricava il JAVADOC
#   invece dei natives. Il canarino taglia (exit 3) ha fermato la build
#   PRIMA di seminare un bundle falso: comportamento corretto.
#   RIMEDI in questo script:
#     (1) selezione asset che ESCLUDE ESPLICITAMENTE *-javadoc.jar e
#         *-sources.jar e richiede UN SOLO candidato (0 o 2+ candidati =
#         errore: mai "il primo dell'elenco");
#     (2) guard sul tag risolto: deve citare "cef-<versione>" (coerenza
#         CefBuildInfo, STEP 3ae) e NON deve finire per -javadoc/-sources;
#     (3) sha256 del jar verificato quando l'API dichiara il digest
#         dell'asset (catch-all contro qualunque asset sbagliato);
#     (4) fallback aggiuntivi (b) e (c) sopra descritti.
#   E2E VALIDATO in locale (nov 2026) con download reale windows-amd64:
#   171,200,308 byte, tar.gz unico nel jar, build_meta.json alla radice
#   del tar.gz, semina e idempotenza OK.
# =============================================================================

set -euo pipefail

log() { echo "[fetch-jcef-natives] $*"; }
die() { # $1=exit code, resto=messaggio
  local code="$1"; shift
  echo "[fetch-jcef-natives] ERRORE: $*" >&2
  exit "$code"
}

# --- 1) argomenti -> piattaforma jcefmaven + sottocartella risorse ----------
SEED_OS="${1:-}"
SEED_ARCH="${2:-}"
[ -n "$SEED_OS" ] && [ -n "$SEED_ARCH" ] \
  || die 2 "uso: fetch-kcef-bundle.sh <seed_os> <seed_arch> (windows|linux|osx + x64|aarch64)"

case "${SEED_OS}:${SEED_ARCH}" in
  windows:x64)  PLATFORM="windows-amd64"; KCEF_SUBDIR="windows" ;;
  linux:x64)    PLATFORM="linux-amd64";   KCEF_SUBDIR="linux" ;;
  osx:aarch64)  PLATFORM="macosx-arm64";  KCEF_SUBDIR="macos" ;;
  osx:x64)      PLATFORM="macosx-amd64";  KCEF_SUBDIR="macos" ;;
  *) die 2 "coppia seed_os/seed_arch non riconosciuta: '${SEED_OS}/${SEED_ARCH}' (attese: windows+x64, linux+x64, osx+x64, osx+aarch64)" ;;
esac
log "platform jcefmaven: ${PLATFORM}  ->  desktop-resources/${KCEF_SUBDIR}/kcef/"

# NB: la cartella macOS DEVE essere "macos" (riconosciuta dalla DSL Compose
# appResourcesRootDir: common|linux|windows|macos) - un seed in "osx/" NON
# verrebbe incluso nell'installer (STEP 3ae).

# --- 2) versione jcefmaven dal toml ------------------------------------------
TOML="gradle/libs.versions.toml"
[ -f "$TOML" ] || die 2 "eseguire dalla RADICE del repo: '${TOML}' non trovato"
JCEFMAVEN_VERSION=$(grep -E '^[[:space:]]*jcefmaven[[:space:]]*=' "$TOML" | head -n 1 \
  | sed 's/^[^=]*=[[:space:]]*//' | tr -d '"' | tr -d "'" | tr -d '[:space:]')
[ -n "$JCEFMAVEN_VERSION" ] || die 2 "chiave 'jcefmaven' non trovata in ${TOML}"
log "versione jcefmaven (gradle/libs.versions.toml): ${JCEFMAVEN_VERSION}"

# --- 3) directory di semina + idempotenza (re-run = skip download) -----------
KCEF_DIR="composeApp/desktop-resources/${KCEF_SUBDIR}/kcef"
mkdir -p "$KCEF_DIR"
EXISTING=$(find "$KCEF_DIR" -maxdepth 1 -type f -name "jcef-natives-${PLATFORM}-*.tar.gz" 2>/dev/null | head -n 1 || true)
if [ -n "$EXISTING" ]; then
  esize=$(wc -c < "$EXISTING")
  if [ "$esize" -ge 52428800 ]; then
    log "bundle gia' seminato: ${EXISTING} (${esize} byte) -> skip download (idempotenza)"
    exit 0
  fi
  log "bundle esistente ma sospetto (${esize} byte): lo sostituisco"
  rm -f "$EXISTING"
fi

TMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/kcef-fetch.XXXXXX")
trap 'rm -rf "$TMP_DIR"' EXIT

PY="$(command -v python3 || command -v python || true)"
log "python per parsing/estrazione: ${PY:-NON disponibile (uso fallback unzip/jar/sed)}"

# --- 4) risoluzione del tag del bundle natives -------------------------------
log "risolvo il tag del bundle natives dalla release GitHub di jcefmaven ${JCEFMAVEN_VERSION}"

API_HEADERS=(-H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28")
if [ -n "${GITHUB_TOKEN:-}" ]; then
  log "GitHub API con token (rate limit elevato)"
  API_HEADERS+=(-H "Authorization: Bearer ${GITHUB_TOKEN}")
else
  log "GitHub API senza token (rate limit anonimo: 60/h per IP)"
fi

API_JSON_FILE="${TMP_DIR}/api.json"
curl -fsSL --max-time 60 --retry 3 --retry-delay 5 "${API_HEADERS[@]}" \
  "https://api.github.com/repos/jcefmaven/jcefmaven/releases/tags/${JCEFMAVEN_VERSION}" \
  -o "$API_JSON_FILE" 2>/dev/null || true

TAG=""
TAG_SHA=""

# (a) GitHub API: UN SOLO candidato natives per la piattaforma, escludendo
# ESPLICITAMENTE le varianti -javadoc.jar e -sources.jar (STEP 3ai)
if [ -n "$PY" ] && [ -s "$API_JSON_FILE" ]; then
  TAG_OUT=$("$PY" -c '
import sys, json, re
platform, path = sys.argv[1], sys.argv[2]
try:
    data = json.load(open(path, encoding="utf-8"))
except Exception as e:
    sys.stderr.write("json API non leggibile: %s\n" % e)
    sys.exit(1)
assets = data.get("assets") or []
names, digests = [], {}
for a in assets:
    if isinstance(a, dict) and a.get("name"):
        names.append(a["name"])
        d = a.get("digest") or ""
        if d.startswith("sha256:"):
            digests[a["name"]] = d.split(":", 1)[1]
pat = re.compile(r"^jcef-natives-" + re.escape(platform) + r"-(.+)\.jar$")
cands = [n for n in names if pat.match(n)]
rej = [n for n in cands if n.endswith("-javadoc.jar") or n.endswith("-sources.jar")]
valid = [n for n in cands if n not in rej]
for r in rej:
    sys.stderr.write("asset scartato (variante javadoc/sources, STEP 3ai): %s\n" % r)
if len(valid) != 1:
    sys.stderr.write("candidati natives %s validi: %d (validi=%r, scartati=%r)\n"
                     % (platform, len(valid), valid, rej))
    sys.exit(1)
print(pat.match(valid[0]).group(1))
print(digests.get(valid[0], ""))
' "$PLATFORM" "$API_JSON_FILE" 2>"$TMP_DIR/tag.err" || true)
  [ -s "$TMP_DIR/tag.err" ] && sed 's/^/[fetch-jcef-natives] /' "$TMP_DIR/tag.err" >&2 || true
  TAG=$(printf '%s\n' "$TAG_OUT" | sed -n '1p')
  TAG_SHA=$(printf '%s\n' "$TAG_OUT" | sed -n '2p')
fi

# (b) fallback: asset build_meta.json della release (2KB, campo release_tag:
# il tag ESATTO senza varianti javadoc/sources; nessun rate limit)
if [ -z "$TAG" ]; then
  log "GitHub API non utile: fallback sull'asset build_meta.json della release"
  BM_FILE="${TMP_DIR}/build_meta.json"
  curl -fsSL --max-time 60 --retry 3 --retry-delay 5 \
    "https://github.com/jcefmaven/jcefmaven/releases/download/${JCEFMAVEN_VERSION}/build_meta.json" \
    -o "$BM_FILE" 2>/dev/null || true
  if [ -s "$BM_FILE" ]; then
    TAG=$(sed -n 's/.*"release_tag"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$BM_FILE" | head -n 1)
  fi
fi

# (c) fallback finale: Maven Central, maven-metadata.xml di
# me.friwi:jcef-natives-<platform> (versioni = tag completi dei bundle)
if [ -z "$TAG" ]; then
  log "fallback finale: maven-metadata.xml di me.friwi:jcef-natives-${PLATFORM} (Maven Central)"
  META=$(curl -fsSL --max-time 60 --retry 5 --retry-delay 10 \
    "https://repo1.maven.org/maven2/me/friwi/jcef-natives-${PLATFORM}/maven-metadata.xml" 2>/dev/null || true)
  if [ -n "$META" ]; then
    TAG=$(printf '%s\n' "$META" | grep -o '<version>[^<]*</version>' | sed 's/<[^>]*>//g' \
      | grep -F "cef-${JCEFMAVEN_VERSION}" | tail -n 1 || true)
  fi
fi

[ -n "$TAG" ] || die 4 "tag del bundle natives non risolvibile per ${PLATFORM} (jcefmaven ${JCEFMAVEN_VERSION}): API, build_meta.json e Maven Central falliti"

# guard STEP 3ai: le varianti javadoc/sources NON sono mai un tag valido
case "$TAG" in
  *-javadoc|*-javadoc.jar|*-sources|*-sources.jar)
    die 4 "tag risolto sospetto (variante javadoc/sources): '${TAG}'" ;;
esac
# guard STEP 3ae: il tag deve citare cef-<versione> del toml, altrimenti
# l'app (CefBuildInfo.fromClasspath()) ignorerebbe il pre-seed
case "$TAG" in
  *"cef-${JCEFMAVEN_VERSION}"*) ;;
  *) die 4 "coerenza tag fallita: '${TAG}' non cita cef-${JCEFMAVEN_VERSION}" ;;
esac
log "release tag dell'archivio: ${TAG}"

# --- 5) download del jar natives (primario GitHub, fallback Maven Central) ---
GH_URL="https://github.com/jcefmaven/jcefmaven/releases/download/${JCEFMAVEN_VERSION}/jcef-natives-${PLATFORM}-${TAG}.jar"
MC_URL="https://repo1.maven.org/maven2/me/friwi/jcef-natives-${PLATFORM}/${TAG}/jcef-natives-${PLATFORM}-${TAG}.jar"
JAR="${TMP_DIR}/natives.jar"
SOURCE=""

log "download jar (primario GitHub): ${GH_URL}"
if curl -fL --max-time 570 --retry 3 --retry-delay 5 -o "$JAR" "$GH_URL"; then
  SOURCE="GitHub"
else
  log "download GitHub fallito: fallback Maven Central (stesse URL di CefAppBuilder)"
  log "download jar (fallback Maven Central): ${MC_URL}"
  curl -fL --max-time 570 --retry 3 --retry-delay 5 -o "$JAR" "$MC_URL" \
    || die 3 "download natives jar fallito da GitHub e da Maven Central"
  SOURCE="Maven Central"
fi

# canarini jar: taglia (>= 50MB), magic zip, sha256 dichiarato dall'API
jsize=$(wc -c < "$JAR")
log "jar scaricato (${SOURCE}): ${jsize} byte"
if [ "$jsize" -lt 52428800 ]; then
  die 3 "jar sospettosamente piccolo (${jsize} byte): atteso >= 50MB. Se e' ~74KB hai scaricato il JAVADOC: selezione asset senza fix STEP 3ai?"
fi
pmagic=$(head -c 2 "$JAR" | od -An -tx1 | tr -d ' \n')
[ "$pmagic" = "504b" ] || die 3 "magic zip (PK) mancante nel jar scaricato (${pmagic})"
if [ -n "$TAG_SHA" ]; then
  if command -v sha256sum >/dev/null 2>&1; then
    actual=$(sha256sum "$JAR" | awk '{print $1}')
  elif command -v shasum >/dev/null 2>&1; then
    actual=$(shasum -a 256 "$JAR" | awk '{print $1}')
  else
    actual=""
    log "WARN: nessun tool sha256 disponibile: check digest saltato"
  fi
  if [ -n "$actual" ] && [ "$actual" != "$TAG_SHA" ]; then
    die 3 "sha256 del jar DIVERSO da quello dichiarato dalla release (atteso ${TAG_SHA}, ricevuto ${actual}): asset corrotto o sostituito"
  fi
  [ -n "$actual" ] && log "sha256 verificato: ${actual}"
fi

# --- 6) estrazione del .tar.gz dal jar (python -> unzip -> jar JDK) ----------
SEED_NAME="jcef-natives-${PLATFORM}-${TAG}.tar.gz"
rm -f "$KCEF_DIR"/jcef-natives-"$PLATFORM"-*.tar.gz 2>/dev/null || true
EXTRACTED=""

if [ -n "$PY" ]; then
  if "$PY" -c '
import sys, zipfile
jar, dest, name = sys.argv[1], sys.argv[2], sys.argv[3]
z = zipfile.ZipFile(jar)
tars = [n for n in z.namelist() if n.endswith(".tar.gz")]
if len(tars) != 1:
    sys.stderr.write("tar.gz attesi 1 nel jar, trovati: %r\n" % (tars,))
    sys.exit(5)
with open(dest.rstrip("/") + "/" + name, "wb") as f:
    f.write(z.read(tars[0]))
' "$JAR" "$KCEF_DIR" "$SEED_NAME" 2>"$TMP_DIR/extract.err"; then
    EXTRACTED="${KCEF_DIR}/${SEED_NAME}"
  else
    [ -s "$TMP_DIR/extract.err" ] && sed 's/^/[fetch-jcef-natives] /' "$TMP_DIR/extract.err" >&2 || true
  fi
fi

if [ -z "$EXTRACTED" ] && command -v unzip >/dev/null 2>&1; then
  log "estrazione con unzip (fallback)"
  (cd "$TMP_DIR" && unzip -o -j "$JAR" "${SEED_NAME}" >/dev/null 2>&1) \
    && [ -f "${TMP_DIR}/${SEED_NAME}" ] \
    && mv "${TMP_DIR}/${SEED_NAME}" "${KCEF_DIR}/${SEED_NAME}" \
    && EXTRACTED="${KCEF_DIR}/${SEED_NAME}" || true
fi

if [ -z "$EXTRACTED" ] && command -v jar >/dev/null 2>&1; then
  log "estrazione con jar JDK (fallback)"
  (cd "$TMP_DIR" && jar xf "$JAR" "${SEED_NAME}") \
    && [ -f "${TMP_DIR}/${SEED_NAME}" ] \
    && mv "${TMP_DIR}/${SEED_NAME}" "${KCEF_DIR}/${SEED_NAME}" \
    && EXTRACTED="${KCEF_DIR}/${SEED_NAME}" || true
fi

[ -n "$EXTRACTED" ] && [ -f "$EXTRACTED" ] \
  || die 5 "estrazione del .tar.gz dal jar fallita (catena python/unzip/jar esaurita)"

# --- 7) canarini bundle seminato ---------------------------------------------
ssize=$(wc -c < "$EXTRACTED")
log "bundle seminato: ${EXTRACTED} (${ssize} byte)"
[ "$ssize" -ge 52428800 ] || die 6 "tar.gz seminato sospettosamente piccolo (${ssize} byte): CEF mancante?"
tar -tzf "$EXTRACTED" >/dev/null 2>&1 || die 6 "tar.gz seminato illeggibile"
if [ -n "$PY" ]; then
  "$PY" -c '
import sys, tarfile
names = tarfile.open(sys.argv[1]).getnames()
ok = ("build_meta.json" in names) or ("./build_meta.json" in names)
sys.exit(0 if ok else 1)
' "$EXTRACTED" || die 6 "build_meta.json NON presente alla radice del tar.gz (richiesto dal pre-seed dell'app)"
else
  tar -tzf "$EXTRACTED" | grep -qE '^\.{0,1}/?build_meta\.json$' \
    || die 6 "build_meta.json NON presente alla radice del tar.gz (richiesto dal pre-seed dell'app)"
fi

echo "✅ CEF pre-seminato: ${KCEF_SUBDIR}/${SEED_NAME} (jcefmaven ${JCEFMAVEN_VERSION}, sorgente ${SOURCE})"

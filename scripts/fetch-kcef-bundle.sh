#!/usr/bin/env bash
# =============================================================================
# fetch-kcef-bundle.sh - STEP 3q: scarica il bundle CEF (jbr_jcef) per SEMINARE
# la build desktop di RiPlay (deb/msi/dmg senza download ~200MB per l'utente).
#
# Seleziona l'archivio con la STESSA logica di KCEFBuilder.GitHub (lib KCEF
# 2025.03.23): release "latest" di github.com/JetBrains/JetBrainsRuntime,
# url nel body che contiene "jcef", filtro sistema + architettura, preferenza
# non-sdk e .tar.gz, primo della lista.
#
# Uso:
#   fetch-kcef-bundle.sh [os] [arch] [destinazione]
#     os:    windows | linux | osx  (default: auto dal sistema)
#     arch:  x64 | aarch64          (default: auto dal sistema)
#     destinazione: default composeApp/desktop-resources/<os>/kcef
#
# Dopo l'esecuzione, costruisci i pacchetti come al solito:
#   ./gradlew packageMsi            (su Windows)
#   ./gradlew packageDeb packageRpm (su Linux)
#   ./gradlew packageDmg            (su macOS)
# L'archivio finisce in resources/kcef dentro l'installer e viene estratto
# da KcefEngine al primo avvio (nessun download per l'utente).
# =============================================================================
set -euo pipefail

OS_IN="${1:-auto}"
ARCH_IN="${2:-auto}"

# --- rilevamento automatico sistema ---
if [ "$OS_IN" = "auto" ]; then
  case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*|Windows*) OS_IN="windows" ;;
    Darwin)                        OS_IN="osx" ;;
    *)                             OS_IN="linux" ;;
  esac
fi
if [ "$ARCH_IN" = "auto" ]; then
  case "$(uname -m)" in
    arm64|aarch64) ARCH_IN="aarch64" ;;
    *)             ARCH_IN="x64" ;;
  esac
fi

case "$OS_IN" in
  windows) DEST_OS="windows" ; MATCH_OS='win|windows' ;;
  linux)   DEST_OS="linux"   ; MATCH_OS='linux' ;;
  osx)     DEST_OS="macos"   ; MATCH_OS='mac|darwin|osx' ;;
  *) echo "OS non supportato: $OS_IN" >&2; exit 1 ;;
esac
case "$ARCH_IN" in
  x64)     MATCH_ARCH='amd64|x86_64|x64' ;;
  aarch64) MATCH_ARCH='aarch64|arm64' ;;
  *) echo "Architettura non supportata: $ARCH_IN" >&2; exit 1 ;;
esac

DEST="${3:-composeApp/desktop-resources/$DEST_OS/kcef}"
mkdir -p "$DEST"

AUTH=()
if [ -n "${GITHUB_TOKEN:-}" ]; then
  AUTH=(-H "Authorization: Bearer $GITHUB_TOKEN")
fi

URL=""
NAME=""

# --- selezione via API (come KCEFBuilder.GitHub) ---
API="https://api.github.com/repos/JetBrains/JetBrainsRuntime/releases/latest"
BODY="$(curl -fsSL "${AUTH[@]}" -H "Accept: application/vnd.github+json" "$API" || true)"

if [ -n "$BODY" ]; then
  URL="$(printf '%s' "$BODY" | grep -oE 'https?://[^" ]+' \
        | grep 'jcef' | grep -v '\.checksum' \
        | grep -Ei "$MATCH_OS" | grep -Ei "$MATCH_ARCH" \
        | { grep -v 'sdk' || true; } \
        | grep '\.tar\.gz$' | head -1 || true)"
fi

# --- FALLBACK senza API (rate limit 403: i link stanno nel BODY del release,
#     gli asset github sono solo i sorgenti) ---
if [ -z "$URL" ]; then
  echo "[fetch-kcef] API non disponibile o nessun match: fallback pagina release" >&2
  TAGPAGE="$(curl -fsSLI -o /dev/null -w '%{url_effective}' \
             "https://github.com/JetBrains/JetBrainsRuntime/releases/latest" || true)"
  TAG="${TAGPAGE##*/}"
  if [ -n "$TAG" ] && [ "$TAG" != "latest" ]; then
    BODY="$(curl -fsSL "https://github.com/JetBrains/JetBrainsRuntime/releases/tag/$TAG" || true)"
    URL="$(printf '%s' "$BODY" | grep -oE '(https?:)?//[^"<> ]+' \
          | grep 'jcef' | grep -v '\.checksum' \
          | grep -Ei "$MATCH_OS" | grep -Ei "$MATCH_ARCH" \
          | { grep -v 'sdk' || true; } \
          | grep '\.tar\.gz$' | head -1 || true)"
    # assolutizza href relativi e protocol-relative
    case "$URL" in
      //*) URL="https:$URL" ;;
      /*)  URL="https://github.com$URL" ;;
    esac
  fi
fi

if [ -z "$URL" ]; then
  echo "[fetch-kcef] ERRORE: nessun archivio jbr_jcef trovato per $OS_IN/$ARCH_IN" >&2
  exit 1
fi

NAME="${URL##*/}"
if [ -f "$DEST/$NAME" ]; then
  echo "[fetch-kcef] gia' presente: $DEST/$NAME"
else
  echo "[fetch-kcef] download $NAME ..."
  curl -fL --retry 3 --progress-bar -o "$DEST/$NAME" "$URL"
fi
echo "[fetch-kcef] seminato in $DEST/$NAME"

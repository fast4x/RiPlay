#!/usr/bin/env bash
# ================================================================
#  RiPlay - Installa APK (Linux)
#  Scarica RiPlay.apk da GitHub e la installa nell'emulatore.
#
#  IMPORTANTE: avvia questo script SOLO dopo che la schermata
#  Android e' visibile nell'emulatore.
#
#  Uso: bash installa_apk.sh
# ================================================================

set -euo pipefail

# -- Configurazione ----------------------------------------------
APP_NAME="RiPlay"
APK_NAME="RiPlay.apk"
APK_GITHUB_URL="https://github.com/fast4x/RiPlay/releases/download/v0.7.80/RiPlay-full-release-0.7.80.apk"

ANDROID_SDK_ROOT="$HOME/.android-sdk"
ADB="$ANDROID_SDK_ROOT/platform-tools/adb"
LOG_FILE="/tmp/riplay-apk.log"

# -- Colori ------------------------------------------------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

info()    { echo -e "${CYAN}  [..]${RESET} $*" | tee -a "$LOG_FILE"; }
success() { echo -e "${GREEN}  [OK]${RESET} $*" | tee -a "$LOG_FILE"; }
warn()    { echo -e "${YELLOW}  [!!]${RESET} $*" | tee -a "$LOG_FILE"; }
error()   { echo -e "${RED}  [XX] ERRORE:${RESET} $*" | tee -a "$LOG_FILE"; exit 1; }

# -- Banner ------------------------------------------------------
clear
echo -e "${BOLD}"
cat << 'BANNER'
  +------------------------------------------+
  |      RiPlay - Installazione APK          |
  +------------------------------------------+
BANNER
echo -e "${RESET}"
echo "  Log: $LOG_FILE"
echo ""

date > "$LOG_FILE"
echo "=== RiPlay APK Installer ===" >> "$LOG_FILE"

# -- Verifica ADB disponibile ------------------------------------
[[ ! -x "$ADB" ]] && error "ADB non trovato. Esegui prima 'bash install.sh'."
success "ADB trovato"

# -- Verifica emulatore in esecuzione ----------------------------
info "Verifica emulatore in esecuzione ..."
DEVICES=$("$ADB" devices 2>/dev/null)
if ! echo "$DEVICES" | grep -q "emulator"; then
  error "Nessun emulatore rilevato. Avvia prima l'emulatore e aspetta che Android sia visibile, poi rilancia questo script."
fi
success "Emulatore rilevato"

# -- Verifica che Android sia pronto (3 controlli istantanei) ---
info "Verifica che Android sia completamente pronto ..."

# Fase 1: kernel
printf "  ${CYAN}[..]${RESET} Controllo kernel ..."
VAL=$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
if [[ "$VAL" != "1" ]]; then
  echo ""
  error "Android non ancora pronto. Aspetta che la schermata Home sia visibile e riprova."
fi
echo -e " ${GREEN}OK${RESET}"

# Fase 2: animazione boot
printf "  ${CYAN}[..]${RESET} Controllo animazione boot ..."
VAL=$("$ADB" shell getprop init.svc.bootanim 2>/dev/null | tr -d '\r' || true)
if [[ "$VAL" != "stopped" ]]; then
  echo ""
  error "Android non ancora pronto. Aspetta che la schermata Home sia visibile e riprova."
fi
echo -e " ${GREEN}OK${RESET}"

# Fase 3: package manager
printf "  ${CYAN}[..]${RESET} Controllo package manager ..."
VAL=$("$ADB" shell pm path android 2>/dev/null | tr -d '\r' || true)
if ! echo "$VAL" | grep -q "package:"; then
  echo ""
  error "Package manager non ancora pronto. Aspetta qualche secondo e riprova."
fi
echo -e " ${GREEN}OK${RESET}"
success "Android completamente pronto"

# -- Download APK ------------------------------------------------
APK_TMP="/tmp/$APK_NAME"
info "Download RiPlay.apk da GitHub ..."
if command -v curl &>/dev/null; then
  curl -L --progress-bar "$APK_GITHUB_URL" -o "$APK_TMP"
elif command -v wget &>/dev/null; then
  wget --show-progress "$APK_GITHUB_URL" -O "$APK_TMP"
else
  error "Né curl né wget disponibili."
fi
[[ ! -f "$APK_TMP" ]] && error "APK non scaricata correttamente."
success "RiPlay.apk scaricata"

# -- Installazione APK -------------------------------------------
info "Installazione RiPlay.apk nell'emulatore ..."
ADB_OUT=$("$ADB" install -r "$APK_TMP" 2>&1)
echo "$ADB_OUT" >> "$LOG_FILE"

if echo "$ADB_OUT" | grep -q "Success"; then
  success "RiPlay installata correttamente"
else
  error "Installazione fallita. Dettagli: $ADB_OUT"
fi

# -- Avvio app ---------------------------------------------------
PACKAGE=$("$ADB" shell pm list packages 2>/dev/null \
  | grep -i "riplay" | head -1 | sed 's/package://' | tr -d '\r' || true)

if [[ -n "$PACKAGE" ]]; then
  info "Avvio RiPlay ($PACKAGE) ..."
  "$ADB" shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >> "$LOG_FILE" 2>&1
  success "RiPlay avviata"
else
  warn "Apri RiPlay manualmente dall'emulatore."
fi

# -- Fine --------------------------------------------------------
echo ""
echo -e "${GREEN}${BOLD}"
cat << 'DONE'
  +------------------------------------------+
  |      RiPlay installata con successo!     |
  +------------------------------------------+
DONE
echo -e "${RESET}"
echo "  RiPlay e' in esecuzione nell'emulatore."
echo "  Per riaprirla in futuro usa l'icona 'RiPlay' sul Desktop."
echo ""

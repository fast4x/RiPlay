#!/usr/bin/env bash
# ================================================================
#  RiPlay Installer - Linux
#  1. Installa Java 17, Android cmdline-tools, system image, AVD
#  2. Avvia l'emulatore e attende il boot completo
#  3. Scarica RiPlay.apk da GitHub e la installa via ADB
#
#  Uso: bash install.sh
#  Requisiti: Ubuntu 20.04+ / Fedora 36+ / Arch, VT-x, 8 GB RAM
# ================================================================

set -euo pipefail

# -- Configurazione ----------------------------------------------
APP_NAME="RiPlay"
APK_NAME="RiPlay.apk"
AVD_NAME="RiPlayDevice"
ANDROID_API="34"
ABI="x86_64"
SYSTEM_IMAGE="system-images;android-${ANDROID_API};google_apis;${ABI}"

# URL ultima release APK da GitHub
# Formato: https://github.com/TUO_UTENTE/TUO_REPO/releases/latest/download/RiPlay.apk
APK_GITHUB_URL="https://github.com/TUO_UTENTE/TUO_REPO/releases/latest/download/RiPlay.apk"

CMDTOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
CMDTOOLS_SHA256="2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92ed5b9e"

ANDROID_SDK_ROOT="$HOME/.android-sdk"
LOG_FILE="/tmp/riplay-install.log"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# -- Colori ------------------------------------------------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

info()    { echo -e "${CYAN}  [..]${RESET} $*" | tee -a "$LOG_FILE"; }
success() { echo -e "${GREEN}  [OK]${RESET} $*" | tee -a "$LOG_FILE"; }
warn()    { echo -e "${YELLOW}  [!!]${RESET} $*" | tee -a "$LOG_FILE"; }
error()   { echo -e "${RED}  [XX] ERRORE:${RESET} $*" | tee -a "$LOG_FILE"; exit 1; }
step()    { echo -e "\n${BOLD}  === Step $1 - $2 ===${RESET}" | tee -a "$LOG_FILE"; }

# -- Variabili derivate ------------------------------------------
CMDTOOLS_DIR="$ANDROID_SDK_ROOT/cmdline-tools/latest"
SDKMANAGER="$CMDTOOLS_DIR/bin/sdkmanager"
AVDMANAGER="$CMDTOOLS_DIR/bin/avdmanager"
EMULATOR="$ANDROID_SDK_ROOT/emulator/emulator"
ADB="$ANDROID_SDK_ROOT/platform-tools/adb"

# -- Banner ------------------------------------------------------
clear
echo -e "${BOLD}"
cat << 'BANNER'
  +------------------------------------------+
  |           RiPlay Installer               |
  |     Android Virtual Device - Linux       |
  +------------------------------------------+
BANNER
echo -e "${RESET}"
echo "  Log: $LOG_FILE"
echo ""

date > "$LOG_FILE"
echo "=== RiPlay Installer ===" >> "$LOG_FILE"

# -- Step 1: Prerequisiti ----------------------------------------
step 1 "Verifica prerequisiti"

if grep -qE 'vmx|svm' /proc/cpuinfo; then
  success "Virtualizzazione hardware (VT-x/AMD-V) abilitata"
else
  warn "Virtualizzazione hardware non rilevata. L'emulatore potrebbe essere lento."
fi

if [ -e /dev/kvm ]; then
  success "KVM disponibile - emulatore accelerato"
else
  warn "KVM non disponibile. Installa: sudo apt install qemu-kvm"
fi

AVAIL_GB=$(df --output=avail "$HOME" | tail -1 | awk '{printf "%d", $1/1024/1024}')
[[ $AVAIL_GB -lt 8 ]] && error "Spazio insufficiente: ${AVAIL_GB} GB (minimo 8 GB)"
success "Spazio disco: ${AVAIL_GB} GB disponibili"

# -- Step 2: Java 17 ---------------------------------------------
step 2 "Verifica Java"

if command -v java &>/dev/null && java -version 2>&1 | grep -qE '"(17|18|19|20|21)'; then
  success "Java gia installato: $(java -version 2>&1 | head -1)"
else
  info "Java 17 non trovato. Installazione in corso ..."
  if command -v apt-get &>/dev/null; then
    sudo apt-get update -qq
    sudo apt-get install -y openjdk-17-jdk-headless unzip curl >> "$LOG_FILE" 2>&1
  elif command -v dnf &>/dev/null; then
    sudo dnf install -y java-17-openjdk-headless unzip curl >> "$LOG_FILE" 2>&1
  elif command -v pacman &>/dev/null; then
    sudo pacman -Sy --noconfirm jdk17-openjdk unzip curl >> "$LOG_FILE" 2>&1
  else
    error "Package manager non supportato. Installa Java 17 manualmente."
  fi
  success "Java 17 installato"
fi

# -- Step 3: Android cmdline-tools -------------------------------
step 3 "Android cmdline-tools"

if [[ -x "$SDKMANAGER" ]]; then
  success "cmdline-tools gia presenti"
else
  info "Download cmdline-tools Google (~150 MB) ..."
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  TMP_ZIP="/tmp/cmdtools.zip"
  curl -L --progress-bar "$CMDTOOLS_URL" -o "$TMP_ZIP"

  info "Verifica integrita SHA256 ..."
  ACTUAL=$(sha256sum "$TMP_ZIP" | awk '{print $1}')
  [[ "$ACTUAL" != "$CMDTOOLS_SHA256" ]] && error "Hash SHA256 non corrispondente per cmdline-tools."
  success "Integrita verificata"

  info "Estrazione ..."
  unzip -q "$TMP_ZIP" -d "$ANDROID_SDK_ROOT/cmdline-tools"
  mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" "$CMDTOOLS_DIR" 2>/dev/null \
    || mv "$ANDROID_SDK_ROOT/cmdline-tools/tools" "$CMDTOOLS_DIR" 2>/dev/null \
    || true
  rm -f "$TMP_ZIP"
  success "cmdline-tools installati"
fi

info "Accettazione licenze SDK ..."
yes | "$SDKMANAGER" --sdk_root="$ANDROID_SDK_ROOT" --licenses >> "$LOG_FILE" 2>&1 || true
success "Licenze accettate"

# -- Step 4: System image + emulatore ----------------------------
step 4 "System image Android $ANDROID_API (~1.5 GB)"

if [[ -d "$ANDROID_SDK_ROOT/system-images/android-${ANDROID_API}" ]]; then
  success "System image Android $ANDROID_API gia presente"
else
  info "Download system image + platform-tools + emulator ..."
  info "(Potrebbe richiedere 5-10 minuti)"
  "$SDKMANAGER" --sdk_root="$ANDROID_SDK_ROOT" \
    "platform-tools" \
    "emulator" \
    "$SYSTEM_IMAGE" >> "$LOG_FILE" 2>&1
  success "System image installata"
fi

# -- Step 5: Crea AVD --------------------------------------------
step 5 "Creazione Android Virtual Device"

if "$AVDMANAGER" list avd 2>/dev/null | grep -q "Name: $AVD_NAME"; then
  success "AVD '$AVD_NAME' gia esistente"
else
  info "Creazione AVD '$AVD_NAME' (Android $ANDROID_API, Pixel Tablet 10.95) ..."
  echo "no" | "$AVDMANAGER" create avd \
    --name "$AVD_NAME" \
    --package "$SYSTEM_IMAGE" \
    --device "pixel_tablet" \
    --force >> "$LOG_FILE" 2>&1

  AVD_CFG="$HOME/.android/avd/${AVD_NAME}.avd/config.ini"
  if [[ -f "$AVD_CFG" ]]; then
    sed -i 's/^hw.ramSize=.*/hw.ramSize=3072/'           "$AVD_CFG" 2>/dev/null || true
    sed -i 's/^vm.heapSize=.*/vm.heapSize=512/'          "$AVD_CFG" 2>/dev/null || true
    sed -i 's/^hw.audioInput=.*/hw.audioInput=yes/'      "$AVD_CFG" 2>/dev/null || true
    sed -i 's/^hw.audioOutput=.*/hw.audioOutput=yes/'    "$AVD_CFG" 2>/dev/null || true
    echo "hw.gpu.enabled=yes"  >> "$AVD_CFG"
    echo "hw.gpu.mode=auto"    >> "$AVD_CFG"
    echo "hw.keyboard=yes"     >> "$AVD_CFG"
    echo "hw.audioInput=yes"   >> "$AVD_CFG"
    echo "hw.audioOutput=yes"  >> "$AVD_CFG"
  fi
  success "AVD '$AVD_NAME' creato e configurato (Pixel Tablet, audio abilitato)"
fi

# -- Step 6: Avvio emulatore -------------------------------------
step 6 "Avvio emulatore Android"

info "Avvio emulatore in background ..."
export ANDROID_SDK_ROOT
export ANDROID_AVD_HOME="$HOME/.android/avd"

"$EMULATOR" -avd "$AVD_NAME" \
  -no-boot-anim \
  -gpu auto \
  -no-snapshot-save >> "$LOG_FILE" 2>&1 &
EMULATOR_PID=$!

info "Attesa boot Android (massimo 5 minuti) ..."
BOOT_TIMEOUT=300
ELAPSED=0

# -- Fase 1: kernel Android avviato --
info "Fase 1/3 - Attesa avvio kernel Android ..."
until "$ADB" shell getprop sys.boot_completed 2>/dev/null | grep -q "^1"; do
  sleep 3; ELAPSED=$((ELAPSED + 3))
  printf "\r  ${CYAN}[..]${RESET} Kernel ... ${ELAPSED}s / ${BOOT_TIMEOUT}s"
  [[ $ELAPSED -ge $BOOT_TIMEOUT ]] && error "Timeout boot (fase 1). Riprova."
done
echo ""
success "Kernel Android pronto (${ELAPSED}s)"

# -- Fase 2: animazione boot terminata --
info "Fase 2/3 - Attesa fine animazione boot ..."
until "$ADB" shell getprop init.svc.bootanim 2>/dev/null | grep -q "stopped"; do
  sleep 2; ELAPSED=$((ELAPSED + 2))
  printf "\r  ${CYAN}[..]${RESET} Animazione boot ... ${ELAPSED}s / ${BOOT_TIMEOUT}s"
  [[ $ELAPSED -ge $BOOT_TIMEOUT ]] && error "Timeout boot (fase 2). Riprova."
done
echo ""
success "Animazione boot terminata (${ELAPSED}s)"

# -- Fase 3: package manager operativo --
info "Fase 3/3 - Attesa avvio package manager ..."
until "$ADB" shell pm path android 2>/dev/null | grep -q "package:"; do
  sleep 2; ELAPSED=$((ELAPSED + 2))
  printf "\r  ${CYAN}[..]${RESET} Package manager ... ${ELAPSED}s / ${BOOT_TIMEOUT}s"
  [[ $ELAPSED -ge $BOOT_TIMEOUT ]] && error "Timeout boot (fase 3). Riprova."
done
echo ""
success "Android completamente pronto in ${ELAPSED}s"


# -- Fine --------------------------------------------------------
echo ""
echo -e "${GREEN}${BOLD}"
cat << 'DONE'
  +------------------------------------------+
  |   Ambiente RiPlay pronto!                |
  +------------------------------------------+
DONE
echo -e "${RESET}"
echo "  L'emulatore Android si sta avviando."
echo ""
echo -e "${YELLOW}  PROSSIMO PASSO:${RESET}"
echo "  Quando vedi la schermata Android, lancia:"
echo "    bash installa_apk.sh"
echo ""
echo "  Log completo: $LOG_FILE"
echo ""

#!/usr/bin/env bash
# ================================================================
#  RiPlay Uninstaller — Linux
#  Rimuove: AVD, Android SDK, Java (opzionale), shortcut Desktop
# ================================================================

set -euo pipefail

APP_NAME="RiPlay"
AVD_NAME="RiPlayDevice"
ANDROID_SDK_ROOT="$HOME/.android-sdk"
JAVA_PKG="openjdk-17-jdk-headless"   # solo se installato da questo script

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

info()    { echo -e "${CYAN}▸${RESET} $*"; }
success() { echo -e "${GREEN}✓${RESET} $*"; }
warn()    { echo -e "${YELLOW}⚠${RESET} $*"; }
ask()     { # Restituisce 0 se l'utente risponde y/Y
  read -rp "  $* [y/N] " ans
  [[ "${ans,,}" == "y" ]]
}

clear
echo -e "${BOLD}"
cat << 'EOF'
  ╔══════════════════════════════════════════╗
  ║      RiPlay Uninstaller  ·  Linux         ║
  ╚══════════════════════════════════════════╝
EOF
echo -e "${RESET}"
warn "Questa operazione rimuoverà RiPlay e l'ambiente Android dal tuo sistema."
echo ""

if ! ask "Vuoi procedere con la disinstallazione?"; then
  echo "  Annullato."
  exit 0
fi
echo ""

# ── 1. Termina emulatore se in esecuzione ───────────────────────
info "Chiusura emulatore Android (se attivo)..."
ADB="$ANDROID_SDK_ROOT/platform-tools/adb"
if [[ -x "$ADB" ]]; then
  "$ADB" emu kill 2>/dev/null || true
  sleep 2
fi
# Forza chiusura processi emulatore residui
pkill -f "emulator.*$AVD_NAME" 2>/dev/null || true
pkill -f "qemu-system"         2>/dev/null || true
success "Emulatore terminato"

# ── 2. Elimina AVD ───────────────────────────────────────────────
info "Rimozione AVD '$AVD_NAME'..."
AVDMANAGER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/avdmanager"
if [[ -x "$AVDMANAGER" ]]; then
  "$AVDMANAGER" delete avd --name "$AVD_NAME" 2>/dev/null || true
fi
# Rimuove anche la cartella .avd residua
rm -rf "$HOME/.android/avd/${AVD_NAME}.avd" 2>/dev/null || true
rm -f  "$HOME/.android/avd/${AVD_NAME}.ini" 2>/dev/null || true
success "AVD rimosso"

# ── 3. Elimina Android SDK ───────────────────────────────────────
if [[ -d "$ANDROID_SDK_ROOT" ]]; then
  SDK_SIZE=$(du -sh "$ANDROID_SDK_ROOT" 2>/dev/null | cut -f1)
  if ask "Rimuovere Android SDK ($SDK_SIZE in $ANDROID_SDK_ROOT)?"; then
    rm -rf "$ANDROID_SDK_ROOT"
    success "Android SDK rimosso"
  else
    warn "Android SDK mantenuto in $ANDROID_SDK_ROOT"
  fi
else
  info "Android SDK non trovato — nulla da rimuovere"
fi

# ── 4. Cartella .android residua ─────────────────────────────────
if [[ -d "$HOME/.android" ]]; then
  if ask "Rimuovere la cartella ~/.android (cache AVD e configurazioni)?"; then
    rm -rf "$HOME/.android"
    success "~/.android rimosso"
  fi
fi

# ── 5. Java (opzionale) ──────────────────────────────────────────
if command -v java &>/dev/null; then
  if ask "Rimuovere Java 17 (potrebbe essere usato da altri programmi)?"; then
    if command -v apt-get &>/dev/null; then
      sudo apt-get remove -y "$JAVA_PKG" 2>/dev/null || true
      sudo apt-get autoremove -y >> /dev/null 2>&1 || true
    elif command -v dnf &>/dev/null; then
      sudo dnf remove -y java-17-openjdk-headless 2>/dev/null || true
    elif command -v pacman &>/dev/null; then
      sudo pacman -Rns --noconfirm jdk17-openjdk 2>/dev/null || true
    fi
    success "Java rimosso"
  else
    warn "Java mantenuto"
  fi
fi

# ── 6. Shortcut e script di lancio ───────────────────────────────
info "Rimozione shortcut e script di lancio..."
rm -f "$HOME/Desktop/${APP_NAME}.desktop"        2>/dev/null || true
rm -f "$HOME/Scrivania/${APP_NAME}.desktop"      2>/dev/null || true
rm -f "$HOME/.local/bin/launch-${APP_NAME,,}.sh" 2>/dev/null || true
success "Shortcut rimossi"

# ── 7. Log installer ─────────────────────────────────────────────
rm -f /tmp/RiPlay-install.log 2>/dev/null || true

# ── Fine ─────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}${BOLD}  ✓ Disinstallazione completata.${RESET}"
echo "  Tutto ciò che era stato installato da RiPlay Installer è stato rimosso."
echo ""

@echo off
:: Launcher per RiPlay Installer
:: Doppio clic su questo file per avviare l'installazione come amministratore

net session >nul 2>&1
if %errorlevel% == 0 goto :run

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "Start-Process -FilePath '%~f0' -Verb RunAs"
exit /b

:run
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0uninstall.ps1"

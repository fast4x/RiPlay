@echo off
REM =============================================================================
REM fetchKcefBundle.cmd - STEP 3q: shim Windows per la semina del bundle CEF
REM (jbr_jcef) nella build desktop. Chiama scripts/fetch-kcef-bundle.ps1 con i
REM parametri di default (windows/x64): l'archivio finisce in
REM composeApp/desktop-resources/windows/kcef/ e viene incluso nell'MSI.
REM
REM Uso:  fetchKcefBundle.cmd
REM =============================================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\fetch-kcef-bundle.ps1" windows x64
if errorlevel 1 exit /b 1
echo.
echo Semina CEF completata: ora puoi costruire l'MSI con  createDesktopPackageMsi.cmd

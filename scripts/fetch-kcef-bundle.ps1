# =============================================================================
# fetch-kcef-bundle.ps1 - STEP 3q: versione WINDOWS dello script di semina del
# bundle CEF (jbr_jcef) per la build desktop di RiPlay (msi senza download
# ~200MB per l'utente). STESSA logica di KCEFBuilder.GitHub (lib KCEF
# 2025.03.23): release "latest" di JetBrains/JetBrainsRuntime, url "jcef" nel
# body, filtro sistema+arch, preferenza non-sdk/.tar.gz, fallback senza API.
#
# Uso:  powershell -File scripts/fetch-kcef-bundle.ps1 [os] [arch] [dest]
#         os: windows | linux | osx   (default windows)
#         arch: x64 | aarch64         (default x64)
# =============================================================================
param(
    [string]$Os = "windows",
    [string]$Arch = "x64",
    [string]$Dest = ""
)

$ErrorActionPreference = "Stop"

switch ($Os) {
    "windows" { $DestOs = "windows"; $MatchOs = 'win|windows' }
    "linux"   { $DestOs = "linux";   $MatchOs = 'linux' }
    "osx"     { $DestOs = "macos";   $MatchOs = 'mac|darwin|osx' }
    default   { throw "OS non supportato: $Os" }
}
switch ($Arch) {
    "x64"     { $MatchArch = 'amd64|x86_64|x64' }
    "aarch64" { $MatchArch = 'aarch64|arm64' }
    default   { throw "Architettura non supportata: $Arch" }
}

if ($Dest -eq "") { $Dest = "composeApp/desktop-resources/$DestOs/kcef" }
New-Item -ItemType Directory -Force -Path $Dest | Out-Null

$headers = @{ "Accept" = "application/vnd.github+json" }
if ($env:GITHUB_TOKEN) {
    $headers["Authorization"] = "Bearer $env:GITHUB_TOKEN"
}

$url = ""
$api = "https://api.github.com/repos/JetBrains/JetBrainsRuntime/releases/latest"
try {
    $body = Invoke-WebRequest -Uri $api -Headers $headers -UseBasicParsing -ErrorAction Stop
    $content = $body.Content
    $candidates = [regex]::Matches($content, 'https?://[^" ]+') |
        ForEach-Object { $_.Value } |
        Where-Object { $_ -match 'jcef' -and $_ -notmatch '\.checksum' } |
        Where-Object { $_ -match $MatchOs -and $_ -match $MatchArch } |
        Where-Object { $_ -notmatch 'sdk' -and $_ -match '\.tar\.gz$' }
    if ($candidates.Count -gt 0) { $url = $candidates[0] }
} catch {
    Write-Host "[fetch-kcef] API non disponibile: $($_.Exception.Message)"
}

if ($url -eq "") {
    Write-Host "[fetch-kcef] fallback pagina release (senza API)"
    # redirect releases/latest -> tag
    $req = [System.Net.HttpWebRequest]::Create("https://github.com/JetBrains/JetBrainsRuntime/releases/latest")
    $req.AllowAutoRedirect = $false
    try { $resp = $req.GetResponse() ; $tagPage = $resp.Headers["Location"] ; $resp.Close() } catch { $tagPage = $_.Response.Headers["Location"] }
    $tag = ($tagPage -split "/")[-1]
    if ($tag -and $tag -ne "latest") {
        $page = (Invoke-WebRequest -Uri "https://github.com/JetBrains/JetBrainsRuntime/releases/tag/$tag" -UseBasicParsing).Content
        $candidates = [regex]::Matches($page, '(https?:)?//[^"<> ]+') |
            ForEach-Object { $_.Value } |
            Where-Object { $_ -match 'jcef' -and $_ -notmatch '\.checksum' } |
            Where-Object { $_ -match $MatchOs -and $_ -match $MatchArch } |
            Where-Object { $_ -notmatch 'sdk' -and $_ -match '\.tar\.gz$' }
        if ($candidates.Count -gt 0) {
            $url = $candidates[0]
            if ($url.StartsWith("//")) { $url = "https:$url" }
            elseif ($url.StartsWith("/")) { $url = "https://github.com$url" }
        }
    }
}

if ($url -eq "") { throw "[fetch-kcef] nessun archivio jbr_jcef trovato per $Os/$Arch" }

$name = ($url -split "/")[-1]
$out = Join-Path $Dest $name
if (Test-Path $out) {
    Write-Host "[fetch-kcef] gia' presente: $out"
} else {
    Write-Host "[fetch-kcef] download $name ..."
    Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing
}
Write-Host "[fetch-kcef] seminato in $out"

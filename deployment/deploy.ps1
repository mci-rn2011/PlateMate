param(
    [string] $RepoPath = $(if ($env:PLATEMATE_REPO_PATH) { $env:PLATEMATE_REPO_PATH } else { Split-Path -Parent $PSScriptRoot }),
    [string] $RuntimePath = $(if ($env:PLATEMATE_RUNTIME_PATH) { $env:PLATEMATE_RUNTIME_PATH } else { "C:\apps\platemate\current" }),
    [string] $AppName = "platemate",
    [string] $JavaHome = $(if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "" }),
    [switch] $SkipGitPull
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $RepoPath)) {
    throw "Repo path does not exist: $RepoPath"
}

Set-Location $RepoPath

if (-not $SkipGitPull) {
    git pull --ff-only
}

function Test-JavaHome {
    param([string] $Path)

    return $Path -and (Test-Path (Join-Path $Path "bin\java.exe"))
}

if (-not (Test-JavaHome $JavaHome)) {
    $JavaHome = ""
    $javaCandidates = @(
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Java\jdk-21.0.2",
        "C:\Program Files\Java\jdk-21*",
        "C:\Program Files\Eclipse Adoptium\jdk-21*",
        "C:\Program Files\Temurin\jdk-21*"
    )

    foreach ($candidate in $javaCandidates) {
        $matches = Get-Item -Path $candidate -ErrorAction SilentlyContinue |
            Where-Object { $_.PSIsContainer -and (Test-JavaHome $_.FullName) } |
            Sort-Object FullName -Descending

        if ($matches) {
            $JavaHome = $matches[0].FullName
            break
        }
    }
}

if (Test-JavaHome $JavaHome) {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
} elseif (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java 21 was not found. Set JAVA_HOME or install JDK 21."
}

$env:VAADIN_USAGE_STATS_ENABLED = "false"

mvn -Pproduction -DskipTests package
if ($LASTEXITCODE -ne 0) {
    throw "Production Maven build failed with exit code $LASTEXITCODE"
}

New-Item -ItemType Directory -Force -Path $RuntimePath | Out-Null
$jarPath = Join-Path $RepoPath "target\platemate-0.0.1-SNAPSHOT.jar"

if (-not (Test-Path $jarPath)) {
    throw "Build artifact was not found: $jarPath"
}

Copy-Item -Path $jarPath `
    -Destination (Join-Path $RuntimePath "platemate.jar") `
    -Force

$env:PLATEMATE_REPO_PATH = $RepoPath
$env:PLATEMATE_RUNTIME_PATH = $RuntimePath

$ecosystemPath = Join-Path $RepoPath "deployment\ecosystem.config.cjs"
$pm2Status = $null
try {
    $pm2Status = pm2 describe $AppName 2>$null
} catch {
    $pm2Status = $null
}

if ($LASTEXITCODE -eq 0 -and $pm2Status) {
    pm2 restart $AppName --update-env
} else {
    pm2 start $ecosystemPath --only $AppName
}

pm2 save

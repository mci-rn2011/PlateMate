param(
    [string] $RepoPath = "C:\apps\platemate\repo",
    [string] $RuntimePath = "C:\apps\platemate\current",
    [string] $AppName = "platemate",
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

$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:VAADIN_USAGE_STATS_ENABLED = "false"

mvn -Pproduction -DskipTests package

New-Item -ItemType Directory -Force -Path $RuntimePath | Out-Null
Copy-Item -Path (Join-Path $RepoPath "target\platemate-0.0.1-SNAPSHOT.jar") `
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

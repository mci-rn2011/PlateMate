$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$pathsToWatch = @(
    (Join-Path $repoRoot "src\main\java"),
    (Join-Path $repoRoot "src\main\resources")
)

Write-Host "Watching Java/resources. Keep scripts\run-dev.ps1 running in another terminal."
Write-Host "When files change, this script runs: mvn compile"

while ($true) {
    $changed = $false
    $watchers = @()
    $jobs = @()

    foreach ($path in $pathsToWatch) {
        $watcher = New-Object System.IO.FileSystemWatcher
        $watcher.Path = $path
        $watcher.IncludeSubdirectories = $true
        $watcher.EnableRaisingEvents = $true

        $jobs += Register-ObjectEvent -InputObject $watcher -EventName Changed -Action { $global:PlateMateChanged = $true }
        $jobs += Register-ObjectEvent -InputObject $watcher -EventName Created -Action { $global:PlateMateChanged = $true }
        $jobs += Register-ObjectEvent -InputObject $watcher -EventName Deleted -Action { $global:PlateMateChanged = $true }
        $jobs += Register-ObjectEvent -InputObject $watcher -EventName Renamed -Action { $global:PlateMateChanged = $true }
        $watchers += $watcher
    }

    while (-not $changed) {
        Start-Sleep -Milliseconds 300
        if ($global:PlateMateChanged) {
            $changed = $true
            $global:PlateMateChanged = $false
        }
    }

    Start-Sleep -Milliseconds 700

    foreach ($job in $jobs) {
        Unregister-Event -SubscriptionId $job.Id
    }
    foreach ($watcher in $watchers) {
        $watcher.Dispose()
    }

    Write-Host "Change detected. Compiling..."
    & mvn compile
}

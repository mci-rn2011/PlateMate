$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        if ($_ -and -not $_.StartsWith("#")) {
            $parts = $_.Split("=", 2)
            if ($parts.Length -eq 2) {
                [Environment]::SetEnvironmentVariable($parts[0], $parts[1], "Process")
            }
        }
    }
}

$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:VAADIN_USAGE_STATS_ENABLED = "false"
$env:SPRING_DEVTOOLS_RESTART_ENABLED = "true"

$vaadinHome = Join-Path $repoRoot "target\vaadin-home"
New-Item -ItemType Directory -Force -Path (Join-Path $vaadinHome ".vaadin") | Out-Null

docker compose up -d

& mvn spring-boot:run `
    "-Dspring-boot.run.profiles=dev" `
    "-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=true -Duser.home=$vaadinHome"

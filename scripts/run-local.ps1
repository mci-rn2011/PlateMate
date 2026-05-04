$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

Get-Content ".env" | ForEach-Object {
    if ($_ -and -not $_.StartsWith("#")) {
        $parts = $_.Split("=", 2)
        if ($parts.Length -eq 2) {
            [Environment]::SetEnvironmentVariable($parts[0], $parts[1], "Process")
        }
    }
}

$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

& "$env:JAVA_HOME\bin\java.exe" `
    -jar "target\platemate-0.0.1-SNAPSHOT.jar" `
    --spring.profiles.active=dev `
    --vaadin.productionMode=true `
    --server.port=$env:PORT `
    --logging.file.name="target\platemate-app.log"

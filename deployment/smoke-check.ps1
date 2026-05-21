param(
    [string] $Url = $(if ($env:PLATEMATE_SMOKE_URL) { $env:PLATEMATE_SMOKE_URL } else { "https://platemate.at" }),
    [int] $Attempts = 18,
    [int] $DelaySeconds = 5
)

$ErrorActionPreference = "Stop"

for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 10

        if ($response.StatusCode -eq 200) {
            Write-Host "Smoke check passed for $Url on attempt $attempt"
            exit 0
        }

        Write-Host "Smoke check attempt $attempt/$Attempts returned status $($response.StatusCode)"
    } catch {
        Write-Host "Smoke check attempt $attempt/$Attempts failed: $($_.Exception.Message)"
    }

    if ($attempt -lt $Attempts) {
        Start-Sleep -Seconds $DelaySeconds
    }
}

throw "Smoke check failed for $Url after $Attempts attempts"

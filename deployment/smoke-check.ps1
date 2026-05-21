param(
    [string] $Url = $(if ($env:PLATEMATE_SMOKE_URL) { $env:PLATEMATE_SMOKE_URL } else { "https://platemate.at" })
)

$ErrorActionPreference = "Stop"

$response = Invoke-WebRequest -Uri $Url -UseBasicParsing

if ($response.StatusCode -ne 200) {
    throw "Smoke check failed for $Url with status $($response.StatusCode)"
}

Write-Host "Smoke check passed for $Url"

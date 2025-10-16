Param(
    [int]$CodeIncrement = 1,
    [string]$NewName
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path | Split-Path -Parent
$propsFile = Join-Path $root 'gradle.properties'
if (!(Test-Path $propsFile)) { Write-Error "gradle.properties not found"; exit 1 }

$content = Get-Content $propsFile
$versionCodeLineIndex = $content.FindIndex({ $_ -match '^VERSION_CODE=' })
$versionNameLineIndex = $content.FindIndex({ $_ -match '^VERSION_NAME=' })

if ($versionCodeLineIndex -lt 0) { Write-Error 'VERSION_CODE not found'; exit 1 }
if ($versionNameLineIndex -lt 0) { Write-Error 'VERSION_NAME not found'; exit 1 }

$currentCode = [int]($content[$versionCodeLineIndex] -replace 'VERSION_CODE=','')
$newCode = $currentCode + $CodeIncrement
$content[$versionCodeLineIndex] = "VERSION_CODE=$newCode"

if ($NewName) {
    $content[$versionNameLineIndex] = "VERSION_NAME=$NewName"
}

Set-Content -Path $propsFile -Value $content -Encoding UTF8
Write-Host "Updated VERSION_CODE -> $newCode"
if ($NewName) { Write-Host "Updated VERSION_NAME -> $NewName" }

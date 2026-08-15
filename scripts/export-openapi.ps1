param(
    [ValidateSet("auth", "user", "course", "assignment", "quiz", "all")]
    [string]$Module = "all",
    [string]$OutputDir = "docs/api"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "Exporting OpenAPI modules=$Module -> $OutputDir"

mvn -q test-compile failsafe:integration-test failsafe:verify `
  "-Dit.test=OpenApiContractExportIT" `
  "-Dopenapi.modules=$Module" `
  "-Dopenapi.outputDir=$OutputDir" `
  "-Dopenapi.verifyCommitted=false" `
  "-Dopenapi.runNegativeChecks=false"

if ($LASTEXITCODE -ne 0) {
    throw "OpenAPI export failed with exit code $LASTEXITCODE"
}

Write-Host "Done. Files written under $OutputDir"
Get-ChildItem $OutputDir -Filter "*.openapi.yaml" | ForEach-Object { Write-Host " - $($_.Name)" }

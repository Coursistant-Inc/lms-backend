param(
    [ValidateSet("auth", "user", "course", "assignment", "quiz", "all")]
    [string]$Module = "all"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$checkDir = "target/openapi-check"
Write-Host "Verifying OpenAPI modules=$Module (export to $checkDir, drift vs docs/api)"

mvn -q test-compile failsafe:integration-test failsafe:verify `
  "-Dit.test=OpenApiContractExportIT" `
  "-Dopenapi.modules=$Module" `
  "-Dopenapi.outputDir=$checkDir" `
  "-Dopenapi.verifyCommitted=true" `
  "-Dopenapi.runNegativeChecks=true"

if ($LASTEXITCODE -ne 0) {
    throw "OpenAPI verify failed with exit code $LASTEXITCODE"
}

Write-Host "OpenAPI verify passed."

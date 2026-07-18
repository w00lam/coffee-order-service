param([switch]$KeepInfrastructure)
. (Join-Path $PSScriptRoot 'common.ps1')

foreach ($pidFile in Get-ChildItem -ErrorAction SilentlyContinue (Join-Path $RuntimeDirectory 'app-*.pid')) {
    $processId = [int](Get-Content -Raw $pidFile.FullName)
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process -and $process.ProcessName -eq 'java') { Stop-Process -Id $processId -Force }
    Remove-Item -LiteralPath $pidFile.FullName -Force
}
if (-not $KeepInfrastructure) { Invoke-LoadTestCompose down -v }

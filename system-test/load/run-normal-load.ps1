param(
    [string]$RunPrefix = ('lt-' + [DateTimeOffset]::UtcNow.ToString('yyyyMMddHHmmss') + '-' + (Get-Random -Maximum 9999).ToString('0000')),
    [int]$ChargeIterations = 60,
    [int]$ChargeAmount = 10,
    [int]$SpendIterations = 80,
    [int]$IdempotentIterations = 30,
    [int]$ConflictIterations = 30,
    [int]$MixedIterations = 120,
    [switch]$SkipObservability
)
$systemTestRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $systemTestRoot 'scripts\common.ps1')

New-Item -ItemType Directory -Force -Path (Join-Path $RuntimeDirectory 'results') | Out-Null
Invoke-SystemTestCompose exec -T postgres psql -U coffee -d coffee_order "--set=prefix=$RunPrefix" -f /system-test/invariants/prepare-run.sql

$env:BASE_URLS = ($SystemTestConfig.AppPorts | ForEach-Object { "http://localhost:$_" }) -join ','
$env:RUN_PREFIX = $RunPrefix
$env:CHARGE_ITERATIONS = [string]$ChargeIterations
$env:CHARGE_AMOUNT = [string]$ChargeAmount
$env:SPEND_ITERATIONS = [string]$SpendIterations
$env:IDEMPOTENT_ITERATIONS = [string]$IdempotentIterations
$env:CONFLICT_ITERATIONS = [string]$ConflictIterations
$env:MIXED_ITERATIONS = [string]$MixedIterations
$summary = Join-Path $RuntimeDirectory "results\$RunPrefix-summary.json"
$raw = Join-Path $RuntimeDirectory "results\$RunPrefix-k6.json"
$invariants = Join-Path $RuntimeDirectory "results\$RunPrefix-invariants.txt"
$collector = $null
$stopFile = Join-Path $RuntimeDirectory "results\$RunPrefix-observability.stop"
if (-not $SkipObservability) {
    Remove-Item -ErrorAction SilentlyContinue -LiteralPath $stopFile
    $observabilityDirectory = Join-Path $RuntimeDirectory "results\$RunPrefix-observability"
    New-Item -ItemType Directory -Force -Path $observabilityDirectory | Out-Null
    $collectorArguments = @(
        '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $SystemTestRoot 'observability\collect-run.ps1'),
        '-RunPrefix', $RunPrefix, '-OutputDirectory', $observabilityDirectory, '-StopFile', $stopFile
    )
    $collector = Start-Process -FilePath 'powershell' -ArgumentList $collectorArguments -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $observabilityDirectory 'collector.out.log') `
        -RedirectStandardError (Join-Path $observabilityDirectory 'collector.err.log')
    Start-Sleep -Seconds 2
}
try {
    & k6 run --out "json=$raw" --summary-export $summary (Join-Path $SystemTestRoot 'load\normal-load.js')
    if ($LASTEXITCODE -ne 0) { throw "k6 failed with exit code $LASTEXITCODE" }
    & (Join-Path $SystemTestRoot 'invariants\verify-invariants.ps1') -RunPrefix $RunPrefix -ExpectedChargeBalance ($ChargeIterations * $ChargeAmount) -OutputPath $invariants
} finally {
    if ($collector) {
        New-Item -ItemType File -Force -Path $stopFile | Out-Null
        if (-not $collector.WaitForExit(60000)) { throw 'Observability collector did not stop within 60 seconds.' }
        $collector.Refresh()
        Remove-Item -ErrorAction SilentlyContinue -LiteralPath $stopFile
        if ($null -ne $collector.ExitCode -and $collector.ExitCode -ne 0) {
            throw "Observability collector failed with exit code $($collector.ExitCode)"
        }
    }
}
Write-Output "NORMAL_LOAD_OK runPrefix=$RunPrefix summary=$summary raw=$raw invariants=$invariants"

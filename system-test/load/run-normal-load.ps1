param(
    [string]$RunPrefix = ('lt-' + [DateTimeOffset]::UtcNow.ToString('yyyyMMddHHmmss') + '-' + (Get-Random -Maximum 9999).ToString('0000')),
    [int]$ChargeIterations = 60,
    [int]$ChargeAmount = 10,
    [int]$SpendIterations = 80,
    [int]$IdempotentIterations = 30,
    [int]$ConflictIterations = 30,
    [int]$MixedIterations = 120
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
& k6 run --out "json=$raw" --summary-export $summary (Join-Path $SystemTestRoot 'load\normal-load.js')
if ($LASTEXITCODE -ne 0) { throw "k6 failed with exit code $LASTEXITCODE" }

& (Join-Path $SystemTestRoot 'invariants\verify-invariants.ps1') -RunPrefix $RunPrefix -ExpectedChargeBalance ($ChargeIterations * $ChargeAmount) -OutputPath $invariants
Write-Output "NORMAL_LOAD_OK runPrefix=$RunPrefix summary=$summary raw=$raw invariants=$invariants"

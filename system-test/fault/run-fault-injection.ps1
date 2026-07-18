param(
    [string]$RunPrefix = ('fault-' + [DateTimeOffset]::UtcNow.ToString('yyyyMMddHHmmss') + '-' + (Get-Random -Maximum 9999).ToString('0000')),
    [int]$Iterations = 120,
    [int]$KafkaOutageSeconds = 5,
    [int]$AppFailureDelaySeconds = 3
)
$systemTestRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $systemTestRoot 'scripts\common.ps1')

$resultsDirectory = Join-Path $RuntimeDirectory 'results'
New-Item -ItemType Directory -Force -Path $resultsDirectory | Out-Null
Invoke-SystemTestCompose exec -T postgres psql -U coffee -d coffee_order "--set=prefix=$RunPrefix" -f /system-test/invariants/prepare-run.sql

$env:BASE_URLS = ($SystemTestConfig.AppPorts | ForEach-Object { "http://localhost:$_" }) -join ','
$env:RUN_PREFIX = $RunPrefix
$env:CHARGE_ITERATIONS = '0'
$env:SPEND_ITERATIONS = [string]$Iterations
$env:IDEMPOTENT_ITERATIONS = '0'
$env:CONFLICT_ITERATIONS = '0'
$env:MIXED_ITERATIONS = [string]$Iterations
$env:SKIP_FUNCTIONAL_THRESHOLD = 'true'
$summary = Join-Path $resultsDirectory "$RunPrefix-summary.json"
$stdout = Join-Path $resultsDirectory "$RunPrefix-k6.log"
$stderr = Join-Path $resultsDirectory "$RunPrefix-k6.err.log"

$startedAt = [DateTimeOffset]::UtcNow
$k6 = Start-Process -FilePath 'k6' -ArgumentList @('run', '--summary-export', $summary, (Join-Path $SystemTestRoot 'load\normal-load.js')) -PassThru -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr
Start-Sleep -Seconds $AppFailureDelaySeconds

$failedPort = $SystemTestConfig.AppPorts[0]
$pidFile = Join-Path $RuntimeDirectory "app-$failedPort.pid"
$failedPid = [int](Get-Content -Raw $pidFile)
$failedProcess = Get-Process -Id $failedPid -ErrorAction SilentlyContinue
if ($failedProcess -and $failedProcess.ProcessName -eq 'java') {
    Stop-Process -Id $failedPid -Force
}
$appFailureAt = [DateTimeOffset]::UtcNow

Invoke-SystemTestCompose stop kafka
$kafkaStoppedAt = [DateTimeOffset]::UtcNow
Start-Sleep -Seconds $KafkaOutageSeconds
Invoke-SystemTestCompose up -d --wait kafka
$kafkaRecoveredAt = [DateTimeOffset]::UtcNow

$k6.WaitForExit()
$k6.Refresh()
if ($null -ne $k6.ExitCode -and $k6.ExitCode -ne 0) { throw "Fault-injection k6 failed with exit code $($k6.ExitCode). See $stderr" }

foreach ($port in $SystemTestConfig.AppPorts | Where-Object { $_ -ne $failedPort }) { Wait-HttpReady -Port $port }
$recoverySeconds = [math]::Round(($kafkaRecoveredAt - $kafkaStoppedAt).TotalSeconds, 3)
$report = [ordered]@{
    runPrefix = $RunPrefix
    startedAtUtc = $startedAt.ToString('o')
    appFailurePort = $failedPort
    appFailureAtUtc = $appFailureAt.ToString('o')
    kafkaStoppedAtUtc = $kafkaStoppedAt.ToString('o')
    kafkaRecoveredAtUtc = $kafkaRecoveredAt.ToString('o')
    kafkaOutageSeconds = $recoverySeconds
    k6Summary = $summary
}
$report | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $resultsDirectory "$RunPrefix-fault-report.json")

& (Join-Path $SystemTestRoot 'invariants\verify-invariants.ps1') -RunPrefix $RunPrefix -ExpectedChargeBalance 0 -TimeoutSeconds 120
Write-Output "FAULT_INJECTION_OK runPrefix=$RunPrefix appFailurePort=$failedPort kafkaOutageSeconds=$recoverySeconds report=$($resultsDirectory)\$RunPrefix-fault-report.json"

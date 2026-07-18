param(
    [ValidateSet('baseline','optimized')][string]$Phase = 'baseline',
    [ValidateSet('low','medium','high','all')][string]$Profile = 'all',
    [ValidateRange(1,20)][int]$Repetitions = 5,
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunSet = 'measured'
)
$systemTestRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $systemTestRoot 'scripts\common.ps1')

$profiles = [ordered]@{
    low = @{ ChargeIterations=30; SpendIterations=40; IdempotentIterations=15; ConflictIterations=15; MixedIterations=60 }
    medium = @{ ChargeIterations=60; SpendIterations=80; IdempotentIterations=30; ConflictIterations=30; MixedIterations=120 }
    high = @{ ChargeIterations=240; SpendIterations=320; IdempotentIterations=120; ConflictIterations=120; MixedIterations=480 }
}
$selected = if ($Profile -eq 'all') { @($profiles.Keys) } else { @($Profile) }
$runIndex = @()
foreach ($profileName in $selected) {
    for ($iteration = 1; $iteration -le $Repetitions; $iteration++) {
        $prefix = "perf-$RunSet-$Phase-$profileName-$($iteration.ToString('00'))"
        $arguments = $profiles[$profileName]
        & (Join-Path $PSScriptRoot 'run-normal-load.ps1') -RunPrefix $prefix @arguments
        if ($LASTEXITCODE -ne 0) { throw "Performance run failed: $prefix" }
        $summaryPath = Join-Path $RuntimeDirectory "results\$prefix-summary.json"
        $summary = Get-Content -Raw $summaryPath | ConvertFrom-Json
        $runIndex += [pscustomobject]@{
            phase=$Phase; profile=$profileName; repetition=$iteration; runPrefix=$prefix
            throughput=$summary.metrics.http_reqs.rate; requests=$summary.metrics.http_reqs.count
            averageMs=$summary.metrics.http_req_duration.avg; p50Ms=$summary.metrics.http_req_duration.med
            p95Ms=$summary.metrics.http_req_duration.'p(95)'; p99Ms=$summary.metrics.http_req_duration.'p(99)'
            checksPassed=$summary.metrics.checks.passes; checksFailed=$summary.metrics.checks.fails
            functionalFailures=$summary.metrics.functional_failures.count
            unexpected5xx=$summary.metrics.unexpected_5xx.count
        }
        $runIndex | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $RuntimeDirectory "results\performance-$Phase-index.json")
    }
}
Write-Output "PERFORMANCE_SUITE_OK phase=$Phase profiles=$($selected -join ',') repetitions=$Repetitions"

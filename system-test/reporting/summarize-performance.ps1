param(
    [ValidateSet('baseline','optimized')][string]$Phase,
    [ValidateSet('low','medium','high','all')][string]$Profile,
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunSet,
    [ValidateRange(1,20)][int]$Repetitions,
    [Parameter(Mandatory = $true)][string]$ResultsDirectory
)
$systemTestRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $systemTestRoot 'load\profiles.ps1')

$runIndex = @()
$selected = if ($Profile -eq 'all') { @($PerformanceProfiles.Keys) } else { @($Profile) }
foreach ($profileName in $selected) {
    for ($iteration = 1; $iteration -le $Repetitions; $iteration++) {
        $prefix = "perf-$RunSet-$Phase-$profileName-$($iteration.ToString('00'))"
        $summaryPath = Join-Path $ResultsDirectory "$prefix-summary.json"
        if (-not (Test-Path $summaryPath)) { continue }
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
    }
}
if ($runIndex.Count -eq 0) { throw "No performance summaries were found for phase=$Phase runSet=$RunSet" }
$indexPath = Join-Path $ResultsDirectory "performance-$Phase-index.json"
$runIndex | ConvertTo-Json | Set-Content -Encoding utf8 $indexPath
Write-Output "PERFORMANCE_INDEX_OK phase=$Phase runs=$($runIndex.Count) path=$indexPath"

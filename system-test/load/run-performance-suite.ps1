param(
    [ValidateSet('baseline','optimized')][string]$Phase = 'baseline',
    [ValidateSet('low','medium','high','all')][string]$Profile = 'all',
    [ValidateRange(1,20)][int]$Repetitions = 5,
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunSet = 'measured'
)
$systemTestRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $systemTestRoot 'scripts\common.ps1')
. (Join-Path $PSScriptRoot 'profiles.ps1')

$effectiveConfigPath = Join-Path $RuntimeDirectory 'effective-config.json'
if (-not (Test-Path $effectiveConfigPath)) {
    throw 'System-test effective config was not found. Run start-environment.ps1 first.'
}
$effectiveConfig = Get-Content -Raw $effectiveConfigPath | ConvertFrom-Json
$expectedPollInterval = if ($Phase -eq 'baseline') { '200ms' } else { '20ms' }
if ($effectiveConfig.outboxPollInterval -ne $expectedPollInterval) {
    throw "Performance phase '$Phase' requires Outbox poll interval $expectedPollInterval, but the running environment uses $($effectiveConfig.outboxPollInterval)."
}

$selected = if ($Profile -eq 'all') { @($PerformanceProfiles.Keys) } else { @($Profile) }
foreach ($profileName in $selected) {
    for ($iteration = 1; $iteration -le $Repetitions; $iteration++) {
        $prefix = "perf-$RunSet-$Phase-$profileName-$($iteration.ToString('00'))"
        $arguments = $PerformanceProfiles[$profileName]
        & (Join-Path $PSScriptRoot 'run-normal-load.ps1') -RunPrefix $prefix @arguments
        if ($LASTEXITCODE -ne 0) { throw "Performance run failed: $prefix" }
        & (Join-Path $SystemTestRoot 'reporting\summarize-performance.ps1') `
            -Phase $Phase -Profile $Profile -RunSet $RunSet -Repetitions $Repetitions `
            -ResultsDirectory (Join-Path $RuntimeDirectory 'results')
    }
}
Write-Output "PERFORMANCE_SUITE_OK phase=$Phase profiles=$($selected -join ',') repetitions=$Repetitions"

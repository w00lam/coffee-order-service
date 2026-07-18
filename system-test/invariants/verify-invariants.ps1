param(
    [Parameter(Mandatory = $true)][string]$RunPrefix,
    [int]$ExpectedChargeBalance = 600,
    [int]$TimeoutSeconds = 90
)
$systemTestRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $systemTestRoot 'scripts\common.ps1')

$deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
do {
    $remaining = Invoke-SystemTestCompose exec -T postgres psql -U coffee -d coffee_order -Atc "select count(*) from order_event_intents event join orders using (order_id) where orders.user_id like '$RunPrefix-%' and event.delivery_state in ('PENDING','PROCESSING')"
    if ([int]($remaining | Select-Object -Last 1) -eq 0) { break }
    Start-Sleep -Milliseconds 250
} while ([DateTimeOffset]::UtcNow -lt $deadline)

if ([DateTimeOffset]::UtcNow -ge $deadline) { throw "Outbox did not drain within $TimeoutSeconds seconds." }
$result = Invoke-SystemTestCompose exec -T postgres psql -U coffee -d coffee_order "--set=prefix=$RunPrefix" "--set=expected_charge_balance=$ExpectedChargeBalance" -f /system-test/invariants/verify-invariants.sql
$result

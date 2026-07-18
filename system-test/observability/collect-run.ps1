param(
    [Parameter(Mandatory = $true)][string]$RunPrefix,
    [Parameter(Mandatory = $true)][string]$OutputDirectory,
    [Parameter(Mandatory = $true)][string]$StopFile,
    [int]$SampleIntervalMilliseconds = 1000
)
$systemTestRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $systemTestRoot 'scripts\common.ps1')

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$startedAt = [DateTimeOffset]::UtcNow
$startedAt.ToString('O') | Set-Content -Encoding ascii (Join-Path $OutputDirectory 'started-at.txt')
Invoke-SystemTestCompose exec -T postgres psql -U coffee -d coffee_order -c 'select pg_stat_statements_reset()' | Out-Null

$metricsPath = Join-Path $OutputDirectory 'application-metrics.jsonl'
$outboxPath = Join-Path $OutputDirectory 'outbox-timeseries.csv'
$header = 'observed_at,pending,processing,failed,published,max_unpublished_age_ms,consumer_events,max_consumer_latency_ms'
$header | Set-Content -Encoding ascii $outboxPath

while (-not (Test-Path $StopFile)) {
    $observedAt = [DateTimeOffset]::UtcNow.ToString('O')
    foreach ($port in $SystemTestConfig.AppPorts) {
        try {
            $body = (Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$port/actuator/prometheus" -TimeoutSec 2).Content
            [ordered]@{ observedAt = $observedAt; instance = $port; prometheus = $body } |
                ConvertTo-Json -Compress | Add-Content -Encoding utf8 $metricsPath
        } catch {
            [ordered]@{ observedAt = $observedAt; instance = $port; error = $_.Exception.Message } |
                ConvertTo-Json -Compress | Add-Content -Encoding utf8 $metricsPath
        }
    }
    $sql = @"
with scoped as (
  select event.* from order_event_intents event join orders using (order_id)
  where orders.user_id like '$RunPrefix-%'
), consumer as (
  select processed.* from popular_menu_processed_events processed join orders using (order_id)
  where orders.user_id like '$RunPrefix-%'
)
select count(*) filter (where delivery_state='PENDING'),
       count(*) filter (where delivery_state='PROCESSING'),
       count(*) filter (where delivery_state='FAILED'),
       count(*) filter (where delivery_state='PUBLISHED'),
       coalesce(extract(epoch from (clock_timestamp()-(min(occurred_at) filter (where delivery_state<>'PUBLISHED'))))*1000,0)::bigint,
       (select count(*) from consumer),
       coalesce((select max(extract(epoch from (processed_at-completed_at))*1000)::bigint from consumer),0)
from scoped
"@
    try {
        $row = Invoke-SystemTestCompose exec -T postgres psql -U coffee -d coffee_order -At -F ',' -c $sql |
            Select-Object -Last 1
        "$observedAt,$row" | Add-Content -Encoding ascii $outboxPath
    } catch {
        "$observedAt,ERROR,$($_.Exception.Message.Replace(',', ';'))" | Add-Content -Encoding utf8 $outboxPath
    }
    Start-Sleep -Milliseconds $SampleIntervalMilliseconds
}

# 종료 신호는 불변식 검증 뒤에 기록되므로 최종 1회 표본은 실행별 counter와 drain 완료 상태를 확정한다.
$observedAt = [DateTimeOffset]::UtcNow.ToString('O')
foreach ($port in $SystemTestConfig.AppPorts) {
    try {
        $body = (Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$port/actuator/prometheus" -TimeoutSec 2).Content
        [ordered]@{ observedAt = $observedAt; instance = $port; prometheus = $body } |
            ConvertTo-Json -Compress | Add-Content -Encoding utf8 $metricsPath
    } catch {
        [ordered]@{ observedAt = $observedAt; instance = $port; error = $_.Exception.Message } |
            ConvertTo-Json -Compress | Add-Content -Encoding utf8 $metricsPath
    }
}
$row = Invoke-SystemTestCompose exec -T postgres psql -U coffee -d coffee_order -At -F ',' -c $sql |
    Select-Object -Last 1
"$observedAt,$row" | Add-Content -Encoding ascii $outboxPath

Invoke-SystemTestCompose exec -T postgres psql -U coffee -d coffee_order --csv -c @"
select queryid, calls, total_exec_time, mean_exec_time, max_exec_time, rows,
       shared_blks_hit, shared_blks_read, temp_blks_written, wal_bytes, query
from pg_stat_statements
where dbid = (select oid from pg_database where datname = 'coffee_order')
order by total_exec_time desc
"@ | Set-Content -Encoding utf8 (Join-Path $OutputDirectory 'postgres-query-stats.csv')

Invoke-SystemTestCompose exec -T postgres psql -U coffee -d coffee_order --csv -c @"
select event.event_id, event.delivery_state, event.attempts,
       extract(epoch from (event.published_at-event.occurred_at))*1000 as publication_latency_ms,
       extract(epoch from (processed.processed_at-event.occurred_at))*1000 as consumer_latency_ms
from order_event_intents event
join orders using (order_id)
left join popular_menu_processed_events processed using (event_id)
where orders.user_id like '$RunPrefix-%'
order by event.occurred_at
"@ | Set-Content -Encoding utf8 (Join-Path $OutputDirectory 'event-latencies.csv')

$lagLines = foreach ($port in $SystemTestConfig.AppPorts) {
    try {
        $body = (Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$port/actuator/prometheus" -TimeoutSec 2).Content
        $body -split "`n" | Where-Object { $_ -match '^kafka_consumer_.+lag' } |
            ForEach-Object { "instance=$port $_" }
    } catch {
        "instance=$port ERROR=$($_.Exception.Message)"
    }
}
$lagLines | Set-Content -Encoding utf8 (Join-Path $OutputDirectory 'consumer-lag.txt')
Invoke-SystemTestCompose exec -T postgres psql -U coffee -d coffee_order --csv -c @"
select wait_event_type, wait_event, state, count(*) from pg_stat_activity
where datname='coffee_order' group by wait_event_type, wait_event, state order by count(*) desc
"@ | Set-Content -Encoding utf8 (Join-Path $OutputDirectory 'postgres-waits.csv')
Invoke-SystemTestCompose logs --since $startedAt.ToString('O') postgres 2>&1 |
    Set-Content -Encoding utf8 (Join-Path $OutputDirectory 'postgres-slow-query.log')
Invoke-SystemTestCompose stats --no-stream 2>&1 |
    Set-Content -Encoding utf8 (Join-Path $OutputDirectory 'container-stats.txt')

Get-ChildItem -File $OutputDirectory | ForEach-Object {
    $hash = (Get-FileHash -Algorithm SHA256 $_.FullName).Hash.ToLowerInvariant()
    [pscustomobject]@{ file = $_.Name; bytes = $_.Length; sha256 = $hash }
} | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $OutputDirectory 'sha256-manifest.json')

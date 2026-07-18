param([switch]$SkipBuild)
. (Join-Path $PSScriptRoot 'common.ps1')

New-Item -ItemType Directory -Force -Path $RuntimeDirectory | Out-Null
if (-not $SkipBuild) {
    & (Join-Path $RepositoryRoot 'gradlew.bat') loadTestBootJar --no-daemon
    if ($LASTEXITCODE -ne 0) { throw 'loadTestBootJar failed.' }
}

Invoke-LoadTestCompose up -d --wait
$jar = Get-ChildItem (Join-Path $RepositoryRoot 'build\libs\*-load-test.jar') | Select-Object -First 1
if (-not $jar) { throw 'Load-test executable jar was not found.' }

foreach ($port in $LoadTestConfig.AppPorts) {
    $stdout = Join-Path $RuntimeDirectory "app-$port.log"
    $stderr = Join-Path $RuntimeDirectory "app-$port.err.log"
    $arguments = @(
        '-jar', $jar.FullName,
        "--server.port=$port",
        "--spring.datasource.url=jdbc:postgresql://localhost:$($LoadTestConfig.PostgresPort)/coffee_order",
        '--spring.datasource.username=coffee', '--spring.datasource.password=coffee',
        "--spring.kafka.bootstrap-servers=localhost:$($LoadTestConfig.KafkaPort)",
        '--coffee.kafka.enabled=true',
        "--coffee.kafka.order-events-topic=$($LoadTestConfig.Topic)",
        "--coffee.kafka.popular-menu-group-id=$($LoadTestConfig.ConsumerGroup)",
        "--coffee.kafka.publisher.poll-interval=$($LoadTestConfig.PollInterval)",
        "--coffee.kafka.publisher.batch-size=$($LoadTestConfig.BatchSize)",
        "--coffee.kafka.publisher.lease-duration=$($LoadTestConfig.LeaseDuration)",
        "--coffee.kafka.publisher.retry-initial-backoff=$($LoadTestConfig.RetryInitialBackoff)",
        "--coffee.kafka.publisher.retry-maximum-backoff=$($LoadTestConfig.RetryMaximumBackoff)",
        '--coffee.kafka.popular-menu.retry-attempts=3',
        '--coffee.kafka.popular-menu.retry-initial-backoff=250ms',
        '--coffee.kafka.popular-menu.retry-multiplier=2',
        '--coffee.kafka.popular-menu.retry-maximum-backoff=2s',
        '--coffee.kafka.popular-menu.retry-topic-suffix=-popular-retry',
        '--coffee.kafka.popular-menu.dlt-topic-suffix=-popular-dlt',
        '--coffee.kafka.popular-menu.auto-create-topics=true'
    )
    $process = Start-Process -FilePath 'java' -ArgumentList $arguments -PassThru -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    Set-Content -Encoding ascii -Path (Join-Path $RuntimeDirectory "app-$port.pid") -Value $process.Id
}

foreach ($port in $LoadTestConfig.AppPorts) { Wait-HttpReady -Port $port }
Write-Output "READY appPorts=$($LoadTestConfig.AppPorts -join ',') postgresPort=$($LoadTestConfig.PostgresPort) kafkaPort=$($LoadTestConfig.KafkaPort)"

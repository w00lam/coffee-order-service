param([switch]$SkipBuild)
. (Join-Path $PSScriptRoot 'common.ps1')

New-Item -ItemType Directory -Force -Path $RuntimeDirectory | Out-Null
foreach ($port in $SystemTestConfig.AppPorts) { Assert-TcpPortAvailable -Port $port }
if (-not $SkipBuild) {
    & (Join-Path $RepositoryRoot 'gradlew.bat') systemTestBootJar --no-daemon
    if ($LASTEXITCODE -ne 0) { throw 'systemTestBootJar failed.' }
}

Invoke-SystemTestCompose up -d --wait
$jar = Get-ChildItem (Join-Path $RepositoryRoot 'build\libs\*-system-test.jar') | Select-Object -First 1
if (-not $jar) { throw 'System-test executable jar was not found.' }

foreach ($port in $SystemTestConfig.AppPorts) {
    $stdout = Join-Path $RuntimeDirectory "app-$port.log"
    $stderr = Join-Path $RuntimeDirectory "app-$port.err.log"
    $arguments = @(
        '-jar', $jar.FullName,
        "--server.port=$port",
        "--spring.datasource.url=jdbc:postgresql://localhost:$($SystemTestConfig.PostgresPort)/coffee_order",
        '--spring.datasource.username=coffee', '--spring.datasource.password=coffee',
        "--spring.kafka.bootstrap-servers=localhost:$($SystemTestConfig.KafkaPort)",
        '--coffee.kafka.enabled=true',
        "--coffee.kafka.order-events-topic=$($SystemTestConfig.Topic)",
        "--coffee.kafka.popular-menu-group-id=$($SystemTestConfig.ConsumerGroup)",
        "--coffee.kafka.publisher.poll-interval=$($SystemTestConfig.PollInterval)",
        "--coffee.kafka.publisher.batch-size=$($SystemTestConfig.BatchSize)",
        "--coffee.kafka.publisher.lease-duration=$($SystemTestConfig.LeaseDuration)",
        "--coffee.kafka.publisher.retry-initial-backoff=$($SystemTestConfig.RetryInitialBackoff)",
        "--coffee.kafka.publisher.retry-maximum-backoff=$($SystemTestConfig.RetryMaximumBackoff)",
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

foreach ($port in $SystemTestConfig.AppPorts) { Wait-HttpReady -Port $port }
Write-Output "READY appPorts=$($SystemTestConfig.AppPorts -join ',') postgresPort=$($SystemTestConfig.PostgresPort) kafkaPort=$($SystemTestConfig.KafkaPort)"

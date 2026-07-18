$SystemTestConfig = [ordered]@{
    ComposeProject = if ($env:COMPOSE_PROJECT_NAME) { $env:COMPOSE_PROJECT_NAME } else { 'coffee-order-system-test' }
    PostgresPort = if ($env:POSTGRES_PORT) { [int]$env:POSTGRES_PORT } else { 15432 }
    KafkaPort = if ($env:KAFKA_PORT) { [int]$env:KAFKA_PORT } else { 19092 }
    AppPorts = if ($env:APP_PORTS) { @($env:APP_PORTS.Split(',') | ForEach-Object { [int]$_.Trim() }) } else { @(18081, 18082, 18083) }
    Topic = if ($env:ORDER_EVENTS_TOPIC) { $env:ORDER_EVENTS_TOPIC } else { 'coffee-order-completed-system-test' }
    ConsumerGroup = if ($env:POPULAR_MENU_GROUP) { $env:POPULAR_MENU_GROUP } else { 'popular-menu-system-test' }
    PollInterval = if ($env:OUTBOX_POLL_INTERVAL) { $env:OUTBOX_POLL_INTERVAL } else { '20ms' }
    BatchSize = if ($env:OUTBOX_BATCH_SIZE) { [int]$env:OUTBOX_BATCH_SIZE } else { 20 }
    LeaseDuration = if ($env:OUTBOX_LEASE_DURATION) { $env:OUTBOX_LEASE_DURATION } else { '5s' }
    RetryInitialBackoff = if ($env:OUTBOX_RETRY_INITIAL_BACKOFF) { $env:OUTBOX_RETRY_INITIAL_BACKOFF } else { '250ms' }
    RetryMaximumBackoff = if ($env:OUTBOX_RETRY_MAXIMUM_BACKOFF) { $env:OUTBOX_RETRY_MAXIMUM_BACKOFF } else { '5s' }
}

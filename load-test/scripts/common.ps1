$ErrorActionPreference = 'Stop'
$script:LoadTestRoot = Split-Path -Parent $PSScriptRoot
$script:RepositoryRoot = Split-Path -Parent $script:LoadTestRoot
. (Join-Path $script:LoadTestRoot 'config.ps1')
$script:RuntimeDirectory = Join-Path $script:RepositoryRoot 'build\load-test'
$script:ComposeFile = Join-Path $script:LoadTestRoot 'compose.yml'

function Invoke-LoadTestCompose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    $env:COMPOSE_PROJECT_NAME = $LoadTestConfig.ComposeProject
    $env:POSTGRES_PORT = [string]$LoadTestConfig.PostgresPort
    $env:KAFKA_PORT = [string]$LoadTestConfig.KafkaPort
    & docker compose -f $script:ComposeFile @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Docker Compose failed with exit code $LASTEXITCODE" }
}

function Wait-HttpReady {
    param([int]$Port, [int]$TimeoutSeconds = 90)
    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$Port/menus" -TimeoutSec 2
            if ($response.StatusCode -eq 200) { return }
        } catch { }
        Start-Sleep -Milliseconds 250
    } while ([DateTimeOffset]::UtcNow -lt $deadline)
    throw "Application on port $Port did not become ready within $TimeoutSeconds seconds."
}

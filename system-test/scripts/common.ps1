$ErrorActionPreference = 'Stop'
$script:SystemTestRoot = Split-Path -Parent $PSScriptRoot
$script:RepositoryRoot = Split-Path -Parent $script:SystemTestRoot
. (Join-Path $script:SystemTestRoot 'config.ps1')
$script:RuntimeDirectory = Join-Path $script:RepositoryRoot 'build\system-test'
$script:ComposeFile = Join-Path $script:SystemTestRoot 'compose.yml'

function Invoke-SystemTestCompose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    $env:COMPOSE_PROJECT_NAME = $SystemTestConfig.ComposeProject
    $env:POSTGRES_PORT = [string]$SystemTestConfig.PostgresPort
    $env:KAFKA_PORT = [string]$SystemTestConfig.KafkaPort
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

function Assert-TcpPortAvailable {
    param([int]$Port)
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
    try {
        $listener.Start()
    } catch {
        throw "TCP port $Port is already in use. Stop the conflicting process or configure APP_PORTS."
    } finally {
        $listener.Stop()
    }
}

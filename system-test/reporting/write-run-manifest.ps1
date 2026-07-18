param(
    [Parameter(Mandatory = $true)][string]$OutputDirectory
)
$manifestPath = Join-Path $OutputDirectory 'sha256-manifest.json'
Get-ChildItem -File $OutputDirectory |
    Where-Object { $_.FullName -ne $manifestPath -and $_.Name -notlike 'collector.*.log' } |
    Sort-Object Name |
    ForEach-Object {
        $hash = (Get-FileHash -Algorithm SHA256 $_.FullName).Hash.ToLowerInvariant()
        [pscustomobject]@{ file = $_.Name; bytes = $_.Length; sha256 = $hash }
    } | ConvertTo-Json | Set-Content -Encoding utf8 $manifestPath

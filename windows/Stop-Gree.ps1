$ErrorActionPreference = 'Stop'
$pidPath = Join-Path $PSScriptRoot 'controller.pid'
if (Test-Path -LiteralPath $pidPath) {
    $savedId = [int](Get-Content -LiteralPath $pidPath -Raw)
    $candidate = Get-Process -Id $savedId -ErrorAction SilentlyContinue
    $expectedPath = Join-Path $PSScriptRoot 'runtime\bin\java.exe'
    if ($candidate -and $candidate.Path -eq $expectedPath) {
        Stop-Process -Id $savedId
        Write-Output 'Gree PC controller stopped. AC settings are unchanged.'
    }
    Remove-Item -LiteralPath $pidPath
}

param([switch]$NoBrowser)
$ErrorActionPreference = 'Stop'
$javaPath = Join-Path $PSScriptRoot 'runtime\bin\java.exe'
$jarPath = Join-Path $PSScriptRoot 'gree-controller.jar'
$pidPath = Join-Path $PSScriptRoot 'controller.pid'
$url = 'http://127.0.0.1:8081/'
$running = $null
if (Test-Path -LiteralPath $pidPath) {
    $savedId = [int](Get-Content -LiteralPath $pidPath -Raw)
    $candidate = Get-Process -Id $savedId -ErrorAction SilentlyContinue
    if ($candidate -and $candidate.Path -eq $javaPath) { $running = $candidate }
}
if (-not $running) {
    $acConfigPath = Join-Path $PSScriptRoot 'ac-address.txt'
    if (-not (Test-Path -LiteralPath $acConfigPath)) {
        $enteredAddress = Read-Host 'Enter the local IP address of your Gree air conditioner'
        $checkedAddress = $null
        if (-not [Net.IPAddress]::TryParse($enteredAddress, [ref]$checkedAddress) -or $checkedAddress.AddressFamily -ne [Net.Sockets.AddressFamily]::InterNetwork) { throw 'Enter a valid IPv4 address.' }
        $enteredAddress | Set-Content -LiteralPath $acConfigPath
    }
    $acAddress = (Get-Content -LiteralPath $acConfigPath -Raw).Trim()
    $parsedAddress = $null
    if (-not [Net.IPAddress]::TryParse($acAddress, [ref]$parsedAddress)) { throw 'ac-address.txt must contain the air conditioner IP address.' }
    $arguments = @("-Dgree.address=$acAddress", '-jar', ('"' + $jarPath + '"'))
    $lanConfigPath = Join-Path $PSScriptRoot 'lan-address.txt'
    if (Test-Path -LiteralPath $lanConfigPath) {
        $lanAddress = (Get-Content -LiteralPath $lanConfigPath -Raw).Trim()
        if ($lanAddress) {
            $parsedLanAddress = $null
            if (-not [Net.IPAddress]::TryParse($lanAddress, [ref]$parsedLanAddress) -or $parsedLanAddress.AddressFamily -ne [Net.Sockets.AddressFamily]::InterNetwork) { throw 'lan-address.txt must contain this PC IPv4 address.' }
            $arguments = @("-Dgree.lan-address=$lanAddress") + $arguments
        }
    }
    $running = Start-Process -FilePath $javaPath -ArgumentList $arguments -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $PSScriptRoot 'controller.log') -RedirectStandardError (Join-Path $PSScriptRoot 'controller-error.log') -PassThru
    $running.Id | Set-Content -LiteralPath $pidPath
}
$ready = $false
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    $running.Refresh()
    if ($running.HasExited) { throw 'Controller stopped. See controller.log and controller-error.log in this folder.' }
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) { $ready = $true; break }
    } catch { Start-Sleep -Milliseconds 1000 }
}
if (-not $ready) { throw 'Controller is not ready. Check the logs and the AC network connection.' }
Write-Output "Gree controller ready: $url"
if (-not $NoBrowser) { Start-Process $url }

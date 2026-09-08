#Requires -RunAsAdministrator
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$RemoteSubnet)
$ErrorActionPreference = 'Stop'
$lanAddress = (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'lan-address.txt') -Raw).Trim()
$parsedAddress = $null
if (-not [Net.IPAddress]::TryParse($lanAddress, [ref]$parsedAddress) -or $parsedAddress.AddressFamily -ne [Net.Sockets.AddressFamily]::InterNetwork -or $lanAddress -eq '0.0.0.0') { throw 'lan-address.txt must contain this PC LAN IPv4 address.' }
$subnetParts = $RemoteSubnet.Split('/')
$parsedSubnet = $null
if ($subnetParts.Count -ne 2 -or -not [Net.IPAddress]::TryParse($subnetParts[0], [ref]$parsedSubnet) -or $parsedSubnet.AddressFamily -ne [Net.Sockets.AddressFamily]::InterNetwork -or $subnetParts[1] -notmatch '^\d+$' -or [int]$subnetParts[1] -lt 8 -or [int]$subnetParts[1] -gt 32) { throw 'Specify a local IPv4 CIDR subnet, for example 192.168.0.0/24.' }
$ruleName = 'GreePC-HomeLAN-8081'
$settings = @{
    DisplayName='Gree PC - home network only (8081)'; Direction='Inbound'; Action='Allow'; Enabled='True'; Profile='Any';
    Protocol='TCP'; LocalPort=8081; LocalAddress=$lanAddress; RemoteAddress=$RemoteSubnet;
    Program=(Join-Path $PSScriptRoot 'runtime\bin\java.exe'); EdgeTraversalPolicy='Block'
}
if (Get-NetFirewallRule -Name $ruleName -ErrorAction SilentlyContinue) { Set-NetFirewallRule -Name $ruleName @settings | Out-Null }
else { New-NetFirewallRule -Name $ruleName @settings | Out-Null }
Write-Output "Allowed $RemoteSubnet to reach http://${lanAddress}:8081/"

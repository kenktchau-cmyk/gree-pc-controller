[CmdletBinding()]
param([string]$OutputDirectory = (Join-Path $PSScriptRoot 'dist'))
$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to a Java 8 JDK first.' }
$javaBinary = Join-Path $env:JAVA_HOME 'bin\java.exe'
$javaVersion = (& $javaBinary -version 2>&1 | Out-String)
if ($javaVersion -notmatch 'version "1\.8\.') { throw 'This legacy Spring Boot application requires Java 8.' }
$javaRuntime = Join-Path $env:JAVA_HOME 'jre'
if (-not (Test-Path -LiteralPath $javaRuntime)) { throw 'The Java 8 JDK must include a jre directory.' }
$maven = Get-Command mvn.cmd -ErrorAction Stop
& $maven.Source -B -ntp '-DforkCount=0' -f (Join-Path $PSScriptRoot 'pom.xml') package
if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$runtimeDestination = Join-Path $OutputDirectory 'runtime'
New-Item -ItemType Directory -Force -Path $runtimeDestination | Out-Null
Get-ChildItem -LiteralPath $javaRuntime -Force | Copy-Item -Destination $runtimeDestination -Recurse -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'target\airconditioner-remote-1.0-SNAPSHOT.jar') -Destination (Join-Path $OutputDirectory 'gree-controller.jar') -Force
Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'windows') -File | Copy-Item -Destination $OutputDirectory -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'README.md') -Destination $OutputDirectory -Force
Write-Output "Ready: $OutputDirectory\Start-Gree.cmd"

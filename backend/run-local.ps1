param([switch]$TestAccounts,[int]$Port=8080)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $repoRoot
$compiler=Get-Command javac -ErrorAction SilentlyContinue
if($compiler){$javaBin=Split-Path $compiler.Source -Parent}
elseif($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))){$javaBin=Join-Path $env:JAVA_HOME 'bin'}
elseif(Test-Path 'C:\Program Files\Java\jdk-27.0.1.0.3\bin\javac.exe'){$javaBin='C:\Program Files\Java\jdk-27.0.1.0.3\bin'}
else{throw 'Cần JDK 17 trở lên. Đặt JAVA_HOME hoặc thêm javac vào PATH.'}
New-Item -ItemType Directory -Force backend/build | Out-Null
& "$javaBin/javac.exe" --release 17 -encoding UTF-8 -d backend/build backend/src/Battle.java backend/src/Json.java backend/src/CloudStore.java backend/src/SocialServer.java backend/src/RoomMatchmaker.java backend/src/GunnyServer.java
if($LASTEXITCODE -ne 0){throw 'Server compilation failed'}
$env:PORT="$Port"
$env:GUNNY_ASSETS=Join-Path $repoRoot 'backend/assets'
if($TestAccounts){$env:GUNNY_TEST_MODE='1'}else{$env:GUNNY_TEST_MODE='0'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build GunnyServer

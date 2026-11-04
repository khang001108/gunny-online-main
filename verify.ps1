$ErrorActionPreference='Stop'
$repoRoot=$PSScriptRoot
Set-Location -LiteralPath $repoRoot
$compiler=Get-Command javac -ErrorAction SilentlyContinue
if($compiler){$javaBin=Split-Path $compiler.Source -Parent}
elseif($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))){$javaBin=Join-Path $env:JAVA_HOME 'bin'}
elseif(Test-Path 'C:\Program Files\Java\jdk-27.0.1.0.3\bin\javac.exe'){$javaBin='C:\Program Files\Java\jdk-27.0.1.0.3\bin'}
else{throw 'Cần JDK 17 trở lên. Đặt JAVA_HOME hoặc thêm javac vào PATH.'}
New-Item -ItemType Directory -Force backend/build | Out-Null
& "$javaBin/javac.exe" --release 17 -encoding UTF-8 -d backend/build backend/src/Battle.java backend/src/Json.java backend/src/CloudStore.java backend/src/GunnyServer.java backend/tests/ServerCheck.java backend/tests/CloudStoreCheck.java backend/tests/RoomSettingsCheck.java backend/tests/Protocol630Check.java backend/tests/AuthContract630Check.java backend/tests/Departure631Check.java
if($LASTEXITCODE -ne 0){throw 'Compilation failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build CloudStoreCheck
if($LASTEXITCODE -ne 0){throw 'Supabase transport checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build AuthContract630Check
if($LASTEXITCODE -ne 0){throw 'Supabase 6.3.1 Auth/reward checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build ServerCheck
if($LASTEXITCODE -ne 0){throw "ServerCheck failed"}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build RoomSettingsCheck
if($LASTEXITCODE -ne 0){throw 'Online server checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Protocol630Check
if($LASTEXITCODE -ne 0){throw '6.3.1 protocol/download checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Departure631Check
if($LASTEXITCODE -ne 0){throw 'Departure checks failed'}

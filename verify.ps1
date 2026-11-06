$ErrorActionPreference='Stop'
$repoRoot=$PSScriptRoot
Set-Location -LiteralPath $repoRoot
$compiler=Get-Command javac -ErrorAction SilentlyContinue
if($compiler){$javaBin=Split-Path $compiler.Source -Parent}
elseif($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))){$javaBin=Join-Path $env:JAVA_HOME 'bin'}
elseif(Test-Path 'C:\Program Files\Java\jdk-27.0.1.0.3\bin\javac.exe'){$javaBin='C:\Program Files\Java\jdk-27.0.1.0.3\bin'}
else{throw 'Cần JDK 17 trở lên. Đặt JAVA_HOME hoặc thêm javac vào PATH.'}
New-Item -ItemType Directory -Force backend/build | Out-Null
& "$javaBin/javac.exe" --release 17 -encoding UTF-8 -d backend/build backend/src/Progression.java backend/src/Battle.java backend/src/ShopCatalog.java backend/src/ItemRules.java backend/src/Json.java backend/src/CloudStore.java backend/src/SocialServer.java backend/src/RoomMatchmaker.java backend/src/LongPollServer.java backend/src/AdminData.java backend/src/AccountWeb.java backend/src/AdminServer.java backend/src/GunnyServer.java backend/tests/ServerCheck.java backend/tests/CloudStoreCheck.java backend/tests/RoomSettingsCheck.java backend/tests/Protocol630Check.java backend/tests/AuthContract630Check.java backend/tests/Departure631Check.java backend/tests/ShotPacket650Check.java backend/tests/Social670ServerCheck.java backend/tests/Matchmaking680Check.java backend/tests/WaitBot690Check.java backend/tests/RoomFix693ServerCheck.java backend/tests/Items710ServerCheck.java backend/tests/RoomCards713ServerCheck.java backend/tests/Shop716ServerCheck.java backend/tests/Arsenal717ServerCheck.java backend/tests/Equipment718ServerCheck.java backend/tests/Equipment718CloudCheck.java backend/tests/Progression719ServerCheck.java backend/tests/Character720ServerCheck.java backend/tests/Service721Check.java backend/tests/Social722Check.java backend/tests/LongPoll730Check.java backend/tests/Admin750Check.java backend/tests/Battle751ServerCheck.java backend/tests/OnlineVisuals.java backend/tests/Battle754Check.java backend/tests/Effects752ServerCheck.java backend/tests/Features753Check.java backend/tests/Auth753Check.java backend/tests/Progression757Check.java backend/tests/Wind760Check.java
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
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build ShotPacket650Check
if($LASTEXITCODE -ne 0){throw 'Complete-shot packet checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Social670ServerCheck
if($LASTEXITCODE -ne 0){throw 'Social/shared-card checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Matchmaking680Check
if($LASTEXITCODE -ne 0){throw 'Room matchmaking checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build WaitBot690Check
if($LASTEXITCODE -ne 0){throw 'Search timeout/bot consensus checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build RoomFix693ServerCheck
if($LASTEXITCODE -ne 0){throw 'Native room correction checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Items710ServerCheck
if($LASTEXITCODE -ne 0){throw 'One-use item checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build RoomCards713ServerCheck
if($LASTEXITCODE -ne 0){throw 'Room card metadata checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Shop716ServerCheck
if($LASTEXITCODE -ne 0){throw 'Shop checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Arsenal717ServerCheck
if($LASTEXITCODE -ne 0){throw 'Arsenal checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Equipment718ServerCheck
if($LASTEXITCODE -ne 0){throw 'Equipment checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Equipment718CloudCheck
if($LASTEXITCODE -ne 0){throw 'Equipment CloudStore checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Progression719ServerCheck
if ($LASTEXITCODE -ne 0) { throw "Progression checks failed" }
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Character720ServerCheck
if ($LASTEXITCODE -ne 0) { throw "Character checks failed" }
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Service721Check
if ($LASTEXITCODE -ne 0) { throw "Service error checks failed" }
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Social722Check
if ($LASTEXITCODE -ne 0) { throw "Social 722 checks failed" }
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build LongPoll730Check
if($LASTEXITCODE -ne 0){throw 'Long polling checks failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Admin750Check
if($LASTEXITCODE -ne 0){throw 'Admin750Check failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Battle751ServerCheck
if($LASTEXITCODE -ne 0){throw 'Battle751ServerCheck failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Effects752ServerCheck
if($LASTEXITCODE -ne 0){throw 'Effects752ServerCheck failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Battle754Check
if($LASTEXITCODE -ne 0){throw 'Battle754Check failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Features753Check
if($LASTEXITCODE -ne 0){throw 'Features753Check failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Auth753Check
if($LASTEXITCODE -ne 0){throw 'Auth753Check failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Progression757Check
if($LASTEXITCODE -ne 0){throw 'Progression757Check failed'}
& "$javaBin/java.exe" '-Djava.awt.headless=true' -cp backend/build Wind760Check
if($LASTEXITCODE -ne 0){throw 'Wind760Check failed'}
if ($LASTEXITCODE -ne 0) { throw "Long polling checks failed" }

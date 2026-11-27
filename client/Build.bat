
@echo off
setlocal
title Gunny Java 7.9.21 - Build Client
cd /d "%~dp0.."

set "TOOLS=D:\gunny\GunnyNokia\tools"

if exist "client\tools\ecj.jar" (
    set "TOOLS=%CD%\client\tools"
)

echo ===============================
echo     BUILD GUNNY JAVA 7.9.21
echo ===============================
echo.

py -3 client\build.py --tools "%TOOLS%"

if errorlevel 1 (
    echo.
    echo [ERROR] Build that bai! Xem thong bao o tren.
    echo Neu file JAR dang duoc mo, hay dong gia lap roi build lai.
) else (
    echo.
    echo [OK] Build thanh cong!
    echo Ban cai chuan nam trong public\downloads.
    explorer "public\downloads"
)

pause

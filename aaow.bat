@echo off
setlocal
if "%~1"=="" (
    call "%~dp0gradlew.bat" :launcher:run --no-daemon --console=plain -q
) else (
    call "%~dp0gradlew.bat" :launcher:run --no-daemon --console=plain -q --args="%*"
)

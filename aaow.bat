@echo off
setlocal EnableDelayedExpansion
set "AAOW_HOME=%~dp0"
if "%AAOW_HOME:~-1%"=="\" set "AAOW_HOME=%AAOW_HOME:~0,-1%"
set "DIST_BAT=%AAOW_HOME%\launcher\build\install\aaow\bin\aaow.bat"

if /I "%~1"=="--rebuild" goto do_rebuild
if not exist "%DIST_BAT%" goto build_first
call "%DIST_BAT%" %*
exit /b %errorlevel%

:do_rebuild
call "%AAOW_HOME%\gradlew.bat" :launcher:installDist --no-daemon --console=plain -q
if errorlevel 1 exit /b %errorlevel%
set "REST_ARGS="
shift
:collect_args
if "%~1"=="" goto after_rebuild
set "REST_ARGS=!REST_ARGS! "%~1""
shift
goto collect_args
:after_rebuild
if defined REST_ARGS call "%DIST_BAT%" !REST_ARGS!
exit /b %errorlevel%

:build_first
call "%AAOW_HOME%\gradlew.bat" :launcher:installDist --no-daemon --console=plain -q
if errorlevel 1 exit /b %errorlevel%
call "%DIST_BAT%" %*
exit /b %errorlevel%

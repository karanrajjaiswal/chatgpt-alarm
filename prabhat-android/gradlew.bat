@echo off
setlocal EnableExtensions
set VERSION=8.13
if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set CACHE=%GRADLE_USER_HOME%\wrapper\dists\prabhat-gradle-%VERSION%
set GRADLE_BIN=
for /r "%CACHE%" %%G in (gradle.bat) do (
  if exist "%%G" if not defined GRADLE_BIN set GRADLE_BIN=%%G
)
if not defined GRADLE_BIN (
  if not exist "%CACHE%" mkdir "%CACHE%"
  echo Downloading Gradle %VERSION% (first build only)...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip' -OutFile '%CACHE%\gradle.zip'; Expand-Archive -Force '%CACHE%\gradle.zip' '%CACHE%'"
  if errorlevel 1 exit /b 1
  set GRADLE_BIN=%CACHE%\gradle-%VERSION%\bin\gradle.bat
)
call "%GRADLE_BIN%" %*
exit /b %ERRORLEVEL%

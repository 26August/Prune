@rem Gradle start up script for Windows
@if "%DEBUG%"=="" @echo off
set DIR=%~dp0
set APP_HOME=%DIR%
set DEFAULT_JVM_OPTS=
set GRADLE_VERSION=8.6
set GRADLE_HOME=%USERPROFILE%\.gradle\wrapper\dists\gradle-%GRADLE_VERSION%-bin
set JAR=%GRADLE_HOME%\gradle-%GRADLE_VERSION%\lib\gradle-launcher-%GRADLE_VERSION%.jar
if exist "%JAR%" (
  java -jar "%JAR%" %*
  exit /b %ERRORLEVEL%
)
where gradle >nul 2>&1 && (gradle %* & exit /b %ERRORLEVEL%)
echo Gradle not found. Install Gradle 8.6 and JDK 17. 1>&2
exit /b 1

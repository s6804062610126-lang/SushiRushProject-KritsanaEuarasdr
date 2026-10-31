@echo off
rem Builds SushiRush.jar (code + images + fonts in one file). Double-click the jar to play.
cd /d "%~dp0"
if exist build rmdir /s /q build
mkdir build\classes
javac --release 17 -encoding UTF-8 -d build\classes src\*.java
if errorlevel 1 (
    echo Compile failed. Make sure Java JDK is installed.
    pause
    exit /b 1
)
rem jar.exe is often not on PATH, so look for it in the JDK folder too
set JAR=jar
where jar >nul 2>nul
if errorlevel 1 (
    if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jar.exe" set JAR="%JAVA_HOME%\bin\jar.exe"
    for /d %%D in ("C:\Program Files\Java\jdk*") do if exist "%%D\bin\jar.exe" set JAR="%%D\bin\jar.exe"
)
%JAR% --create --file SushiRush.jar --main-class Main -C build\classes . assets
if errorlevel 1 (
    echo Building the jar failed.
    pause
    exit /b 1
)
rmdir /s /q build
echo Done: SushiRush.jar

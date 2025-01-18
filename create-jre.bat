@echo off
chcp 437 > nul
echo Creating minimal JRE...

:: Check JAVA_HOME
if "%JAVA_HOME%" == "" (
    echo Error: JAVA_HOME environment variable is not set
    echo Please set JAVA_HOME to point to your Java installation directory
    pause
    exit /b 1
)

:: Check if we're pointing to JDK or JRE
set JRE_PATH=%JAVA_HOME%
if exist "%JAVA_HOME%\jre" (
    set JRE_PATH=%JAVA_HOME%\jre
) else if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Error: Invalid Java installation directory
    echo Please make sure JAVA_HOME points to a valid Java installation
    pause
    exit /b 1
)

:: Clean existing directory
if exist "jre8-minimal" (
    echo Cleaning existing jre8-minimal directory...
    rd /s /q "jre8-minimal"
)

:: Create directory structure
echo Creating directory structure...
mkdir jre8-minimal
mkdir jre8-minimal\bin
mkdir jre8-minimal\lib
mkdir jre8-minimal\lib\amd64
mkdir jre8-minimal\lib\ext
mkdir jre8-minimal\lib\security
mkdir jre8-minimal\bin\server

:: Copy core files
echo Copying core files...
xcopy /Y /E "%JRE_PATH%\bin\*" "jre8-minimal\bin\" > nul
xcopy /Y /E "%JRE_PATH%\lib\*" "jre8-minimal\lib\" > nul

:: Copy specific configuration files
if exist "%JRE_PATH%\lib\amd64\jvm.cfg" (
    copy /Y "%JRE_PATH%\lib\amd64\jvm.cfg" "jre8-minimal\lib\amd64\" > nul
)

:: Create a default jvm.cfg if it doesn't exist
if not exist "jre8-minimal\lib\amd64\jvm.cfg" (
    echo Creating default jvm.cfg...
    echo -server KNOWN > "jre8-minimal\lib\amd64\jvm.cfg"
    echo -client IGNORE >> "jre8-minimal\lib\amd64\jvm.cfg"
)

echo Minimal JRE creation completed!
echo Location: %CD%\jre8-minimal

:: Test the minimal JRE
echo Testing minimal JRE...
jre8-minimal\bin\java -version
if errorlevel 1 (
    echo Warning: JRE test failed. The minimal JRE might be incomplete.
    echo Current JAVA_HOME: %JAVA_HOME%
    echo Used JRE path: %JRE_PATH%
) else (
    echo JRE test successful!
)

pause
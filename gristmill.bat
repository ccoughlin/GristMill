@echo off
REM gristmill - runs the GristMill Region Of Interest (ROI) detection application

REM Maven requires JAVA_HOME environment variable
if "%JAVA_HOME%"=="" (
    echo Please set the environment variable JAVA_HOME to point to the JDK root folder e.g. "SET JAVA_HOME=C:\Program Files\Java\jdk1.8.0_91"
    EXIT /B 1
)

REM Check if Maven found in path
for %%X in (mvn) do (set FOUND=%%~$PATH:X)
if not defined FOUND (
    echo Unable to find Maven executable.
    echo Please add the Apache Maven folder to the path e.g. "SET PATH=%%PATH%%;C:\apache-maven-3.3.9\bin"
    EXIT /B 1
)

echo.
REM Run the application
call mvn.cmd exec:java -Dexec.mainClass="com.emphysic.myriad.gristmill.Main" -Dexec.args=gristmill.conf
EXIT /B 0


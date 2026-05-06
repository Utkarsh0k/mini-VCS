@echo off

echo ============================
echo Building Core...
echo ============================

:: Delete old class files
del /Q *.class 2>nul

:: Compile Java source
javac *.java

if %errorlevel% neq 0 (
    echo.
    echo Build failed
    pause
    exit /b
)

:: Create executable jar
jar cfe core.jar core *.class

echo.
echo Build successful!
echo core.jar generated
echo.

pause
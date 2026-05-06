@echo off

echo ============================
echo Installing Core...
echo ============================

:: Create install folder
mkdir "C:\Core" 2>nul

:: Copy jar
copy /Y core.jar "C:\Core\core.jar"

:: Create launcher
(
echo @echo off
echo java -jar "C:\Core\core.jar" %%*
) > "C:\Core\core.bat"

echo.
echo Core files installed in:
echo C:\Core
echo.
echo NEXT STEP:
echo Add C:\Core to Windows PATH manually
echo.
pause
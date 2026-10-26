@echo off
chcp 65001 >nul 2>&1
cd /d "%~dp0.."

set "PS=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if not exist "%PS%" goto nops

"%PS%" -NoProfile -ExecutionPolicy Bypass -File "tools\check-api.ps1" %*
set RC=%ERRORLEVEL%
echo.
if %RC%==0 (
    echo [DONE] All checks passed.
) else (
    echo [FAILED] Something is missing, see above.
)
echo.
pause
exit /b %RC%

:nops
echo.
echo   PowerShell not found at:
echo   %PS%
echo.
pause
exit /b 1

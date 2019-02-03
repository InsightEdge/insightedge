@echo off
SETLOCAL EnableDelayedExpansion
call %~dp0..\insightedge\conf\insightedge-env.cmd
FOR /F "tokens=*" %%i IN ('"%JAVACMD% -cp "%XAP_HOME%\lib\required\*";"%XAP_HOME%\tools\cli\*"" org.insightedge.cli.commands.I9ECommandFactory cli-insightedge') DO set GS_COMMAND=%%i %*
if "!GS_COMMAND:~0,6!"=="Error:" (
  echo %GS_COMMAND%
) else (
  if "%GS_VERBOSE%"=="true" (
    echo Executing GigaSpaces command:
    echo %GS_COMMAND%
    echo --------------------------------------------------------------------------------
  )
  %GS_COMMAND%
)
endlocal

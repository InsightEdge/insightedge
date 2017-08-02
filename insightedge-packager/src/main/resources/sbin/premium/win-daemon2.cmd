@echo off

rem Runs a Windows command as a daemon. Can work only with java.exe processes

set INSIGHTEDGE_HOME=%~dp0..

rem Test that an argument was given
if "x%2"=="x" (
  echo Usage: win-daemon.cmd ^(start^|stop^) ^<win-title^> ^<win-command^> ^<args...^>
  exit /b 1
)

rem first token will be written to %%a, second to %%b and remaining string to %%c
for /f "tokens=1,2,*" %%a in ("%*") do (
  set MODE=%%a
  set TITLE=%%b
  set COMMAND=%%c
)

set PROCESSNAME=cmd.exe
set PID_DIR=%TEMP%\win-daemon
set PID_FILE=%PID_DIR%\%TITLE%.pid
set LOG_DIR=%INSIGHTEDGE_HOME%\logs

if "x%MODE%"=="xstart" (
  if "x%COMMAND%"=="x" (
    echo Command must be specified
    exit /b 1
  )

  if not exist %PID_DIR% (
    mkdir %PID_DIR%
  )
  if not exist %LOG_DIR% (
    mkdir %LOG_DIR%
  )

  rem check if process is already running and exit
  if exist %PID_FILE% (
    set /p RAW_COMMA_PIDS=<%PID_FILE%
	rem removes the spaces from RAW_PID
	set PIDS=!RAW_COMMA_PIDS: =!
	rem iterates over pids
	for %%i in (!PIDS!) do (
	  set PID=%%i
	  for /f "tokens=1,2" %%a in ('tasklist ^| findstr !PID!') do (
        if "x%%b"=="x!PID!" (
		  echo Process "%TITLE%" is already running, pid: !PIDS!
		  echo You can stop it by calling: win-daemon.cmd stop %TITLE%
		  exit /b 1
	    )
      )
	)
  )

  rem remember PIDs of all currently running processes, init to non-empty string to make string replacement work
  set OLDPIDS=-
  for /f "tokens=1,2" %%a in ('tasklist ^| findstr %PROCESSNAME%') do (
    rem build dash-separated string, the separator does not matter in script
    set OLDPIDS=%%b-!OLDPIDS!
  )
  
  set LOG_FILE=!LOG_DIR!\!TITLE!-%RANDOM%.log
  echo Starting "%TITLE%", logs: !LOG_FILE!
  echo ^> %INSIGHTEDGE_HOME%\%COMMAND%
  start /b %~dp0win-daemon3.cmd !LOG_FILE! %INSIGHTEDGE_HOME%\%COMMAND% ^& exit /b
  
  set TIMEOUT=5
  set CURRENT_TIME=0
  :READ_PIDS
  rem read all tasks and write all new processes PIDs to a comma-separated string
  set PIDS=
  for /f "tokens=1,2" %%a in ('tasklist ^| findstr %PROCESSNAME%') do (
    rem check if OLDPIDS contains PID by replacing it in OLDPIDS with nothing and comparing to old value
    rem syntax: %MYSTRING:OLD_PART=NEW_PART% or !MYSTRING:OLD_PART=NEW_PART!
    if "x!OLDPIDS:%%b=!"=="x!OLDPIDS!" (
	  if "x!PIDS!"=="x" (
	    set PIDS=%%b
	  ) else (
	    set PIDS=!PIDS!,%%b
	  )
	)
  )

  rem waiting is risky, but java.exe process cannot spawn instantly, so we sometimes have to wait some time before it starts
  if "x!PIDS!"=="x" (
	set /a CURRENT_TIME=!CURRENT_TIME!+1
	if !CURRENT_TIME! gtr !TIMEOUT! (
	  echo Timeout reached for the "%TITLE%" to start
	  exit /b 1
	)
	echo Waiting for "%TITLE%" process to start ^(!CURRENT_TIME!/!TIMEOUT!^)...
    timeout 1 > NUL
	goto :READ_PIDS
  )

  rem write PIDs to a temp file
  echo !PIDS! 1>%PID_FILE%
  echo Started "%TITLE%" with pid: !PIDS!

  rem wait before exiting so output can catch up
  timeout 1 > NUL
  exit /b
)


if "x%MODE%"=="xstop" (
  if not exist %PID_FILE% (
	echo PID file not found: %PID_FILE%
    exit /b 1
  )
    
  set TERMINATED=false
  set /p RAW_COMMA_PIDS=<%PID_FILE%
  rem removes the spaces from RAW_PID
  set PIDS=!RAW_COMMA_PIDS: =!
  rem iterates over pids
  for %%i in (!PIDS!) do (
    set PID=%%i
    for /f "tokens=1,2" %%a in ('tasklist ^| findstr !PID!') do (
      if "x%%b"=="x!PID!" (
        echo Stopping "%TITLE%", pid: !PID!
        taskkill /F /T /PID !PID!
		set TERMINATED=true
      )
    )
  )

  del %PID_FILE%
  
  if "x!TERMINATED!"=="xfalse" (
    echo Process "%TITLE%" not found, pid: !PIDS!
    exit /b 1
  )
  exit /b
)


echo Invalid mode: %MODE%, expected start^|stop
exit /b 1
@echo off

rem Runs a Windows command as a daemon. Can work only with java.exe processes

set INSIGHTEDGE_HOME=%~dp0..

rem Test that an argument was given
if "x%2"=="x" (
  echo Usage: win-daemon.cmd ^(start^|stop^) ^<win-title^> ^<win-command^> ^<args...^>
  exit /b 1
)

for /f "tokens=1,2,*" %%a in ("%*") do (
  set MODE=%%a
  set TITLE=%%b
  set COMMAND=%%c
)

set PROCESSNAME=java.exe
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

  set OLDPIDS=
  for /f "tokens=1,2" %%a in ('tasklist ^| findstr java.exe') do (
    set OLDPIDS=%%b-!OLDPIDS!
  )
  
  set LOG_FILE=!LOG_DIR!\!TITLE!-%RANDOM%.log
  echo Starting "%TITLE%", logs: !LOG_FILE!
  echo ^> %INSIGHTEDGE_HOME%\%COMMAND%
  start /b %~dp0win-daemon3.cmd !LOG_FILE! %INSIGHTEDGE_HOME%\%COMMAND% ^& exit /b
  
  rem waiting is risky, but java.exe process cannot spawn instantly, so we have to wait at least some time
  timeout 1 > NUL
  
  set PIDS=
  for /f "tokens=1,2" %%a in ('tasklist ^| findstr java.exe') do (
    if "x!OLDPIDS:%%b=!"=="x!OLDPIDS!" (
	  if "x!PIDS!"=="x" (
	    set PIDS=%%b
	  ) else (
	    set PIDS=!PIDS!,%%b
	  )
	)
  )
  echo !PIDS! 1>%PID_FILE%
  echo Started "%TITLE%" with pid: !PIDS!
      
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
        taskkill /F /PID !PID!
		set TERMINATED=true
      )
    )
  )
  
  if "x!TERMINATED!"=="xfalse" (
    echo Process "%TITLE%" not found, pid: !PIDS!
    exit /b 1
  )
  exit /b
)

echo Invalid mode: %MODE%, expected start^|stop
exit /b 1
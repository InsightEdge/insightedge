@echo off
SETLOCAL EnableDelayedExpansion
call %~dp0..\conf\insightedge-env.cmd

for /f "tokens=1,2" %%a in (%XAP_HOME%\insightedge\VERSION) do (
  if "x%%a"=="xVersion:" (
    set INSIGHTEDGE_VERSION=%%b
  )
)

echo    _____           _       _     _   ______    _
echo   ^|_   _^|         ^(_^)     ^| ^|   ^| ^| ^|  ____^|  ^| ^|
echo     ^| ^|  _ __  ___ _  __ _^| ^|__ ^| ^|_^| ^|__   __^| ^| __ _  ___
echo     ^| ^| ^| '_ \/ __^| ^|/ _` ^| '_ \^| __^|  __^| / _` ^|/ _` ^|/ _ \
echo    _^| ^|_^| ^| ^| \__ \ ^| ^(_^| ^| ^| ^| ^| ^|_^| ^|___^| ^(_^| ^| ^(_^| ^|  __/
echo   ^|_____^|_^| ^|_^|___/_^|\__, ^|_^| ^|_^|\__^|______\__,_^|\__, ^|\___^|
echo                       __/ ^|                       __/ ^|
echo                      ^|___/                       ^|___/   version: %INSIGHTEDGE_VERSION%
echo.

if [%1]==[] goto help
if %1==help goto help
if %1==demo goto demo
if %1==run goto run
if %1==deploy-space goto deploy_space
if %1==undeploy goto undeploy
if %1==shutdown goto shutdown
echo Unknown command %1
goto help

:help
echo Usage: %~n0 [command] [args]
echo Available commands:
echo   demo
echo       Starts a demo environment on the local host
echo   run --master
echo       Runs Spark Master and XAP Manager
echo   run --worker [--containers=n]
echo       Runs Spark Worker and n XAP Containers (default n=zero)
echo   run --zeppelin
echo       Runs Apache Zeppelin
echo   deploy-space [--partitions=x [--backups]] space-name
echo       Deploys a space with the specified name and partitions/backups (Optional)
echo   undeploy space-name
echo       Undeploys space with the specified name
echo   shutdown
echo       Shuts down InsightEdge environment on the local host
exit /B

:demo
echo Starting gs-agent with local manager, spark master, spark worker and 2 containers...
start "InsightEdge Agent" "%XAP_HOME%\bin\gs-agent" --manager-local --spark_master --spark_worker --gsc=2
rem Wait for GSM up to 60 seconds (6 attempts with default timeout of 10 seconds):
FOR /L %%G IN (1,1,6) DO (
  echo !time!
  echo Waiting for master [%%G of 6]...
  "%XAP_HOME%\bin\gs" list gsm | findstr /C:"Found 0 GSMs"
  echo !ERRORLEVEL!
  echo !ERRORLEVEL!
  if !ERRORLEVEL!==1 goto demo_gsm_found
)
echo Aborting - Failed to find GSM
exit /B
:demo_gsm_found
if not defined INSIGHTEDGE_DEMO_SPACE_NAME set INSIGHTEDGE_DEMO_SPACE_NAME=insightedge-space
echo Deploying space %INSIGHTEDGE_DEMO_SPACE_NAME% with 2 partitions...
call "%XAP_HOME%\bin\gs" deploy-space -cluster schema=partitioned total_members=2 %INSIGHTEDGE_DEMO_SPACE_NAME%
echo Starting Zeppelin...
start "InsightEdge Zeppelin" "%XAP_HOME%\insightedge\zeppelin\bin\zeppelin.cmd"
echo **************************************************
echo Demo environment started:
echo - Spark Master: http://localhost:8080
echo - XAP Manager: http://localhost:8090
echo - Zeppelin: http://localhost:%ZEPPELIN_PORT%
echo **************************************************
exit /B

:run
if [%2]==[] (echo Nothing to run & goto run_usage)
if %2==--master (
  set INSIGHTEDGE_CMD=--manager --spark_master
  goto run_agent
)
if %2==--worker (
  set INSIGHTEDGE_CMD=--spark_worker
  if [%3]==[--containers] (
    set INSIGHTEDGE_CMD=%INSIGHTEDGE_CMD% --gsc
    if not [%4]==[] set INSIGHTEDGE_CMD=%INSIGHTEDGE_CMD%=%4
  )
  goto run_agent
)
if %2==--zeppelin (
  echo Running Zeppelin...
  call "%XAP_HOME%\insightedge\zeppelin\bin\zeppelin.cmd"
  exit /B
)
echo Unsupported run option %2 & goto run_usage
:run_agent
echo Starting gs-agent with %INSIGHTEDGE_CMD%...
call "%XAP_HOME%\bin\gs-agent" %INSIGHTEDGE_CMD%
exit /B
:run_usage
echo   %~n0 run --master                  - Runs Spark Master and XAP Manager
echo   %~n0 run --worker [--containers=N] - Runs Spark Worker and N XAP Containers (default N=zero)
echo   %~n0 run --zeppelin                - Runs Apache Zeppelin
exit /B

:deploy_space
if [%2]==[] (
  echo Space name must be specified
  echo Usage: %~n0 deploy-space [--partitions=x [--backups]] space-name
  exit /B
)
if %2==--partitions goto deploy_space_partitions
if %2==--backups goto deploy_space_backups
if defined INSIGHTEDGE_PARTITIONS (
  set INSIGHTEDGE_TOPOLOGY=-cluster schema=partitioned total_members=%INSIGHTEDGE_PARTITIONS%
  if defined INSIGHTEDGE_BACKUPS set INSIGHTEDGE_TOPOLOGY=!INSIGHTEDGE_TOPOLOGY!,%INSIGHTEDGE_BACKUPS%
)
echo Deploying space %2 with [%INSIGHTEDGE_TOPOLOGY%]...
call "%XAP_HOME%\bin\gs" deploy-space %INSIGHTEDGE_TOPOLOGY% %2
exit /B
:deploy_space_partitions
shift
set INSIGHTEDGE_PARTITIONS=%2
shift
goto deploy_space
:deploy_space_backups
set INSIGHTEDGE_BACKUPS=1
shift
goto deploy_space

:undeploy
if [%2]==[] (
  echo Space name must be specified
  echo Usage: %~n0 undeploy space-name
) else (
  echo Undeploying %2...
  call "%XAP_HOME%\bin\gs" undeploy %2
)
exit /B

:shutdown
echo Killing gs-agent...
taskkill /FI "WINDOWTITLE eq GigaSpaces Technologies Service Grid : GSA" /T /F
echo Killing Zeppelin...
taskkill /FI "WINDOWTITLE eq InsightEdge Zeppelin*" /T /F
exit /B
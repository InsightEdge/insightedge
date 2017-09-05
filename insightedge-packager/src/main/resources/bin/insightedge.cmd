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
echo   run --master                                   ^| Runs Spark Master and XAP Manager
echo   run --worker [--containers=n]                  ^| Runs Spark Worker and n XAP Containers (default n=zero)
echo   run --zeppelin                                 ^| Runs Apache Zeppelin
echo   deploy-space [--topology=n,m] space-name       ^| Deploys a space with the specified name
echo   undeploy space-name                            ^| Undeploys space with the specified name
echo   demo                                           ^| Starts a demo environment on the local host
echo   shutdown                                       ^| Shuts down InsightEdge environment on the local host
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
echo Deploying space 'insightedge' with 2 partitions...
call "%XAP_HOME%\bin\gs" deploy-space -cluster schema=partitioned total_members=2 insightedge
echo Starting Zeppelin...
start "InsightEdge Zeppelin" "%XAP_HOME%\insightedge\zeppelin\bin\zeppelin.cmd"
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
set INSIGHTEDGE_CMD=deploy-space
:deploy_space_args
if [%2]==[] (echo Space name must be specified & goto deploy_space_usage)
if %2==--topology goto deploy_space_topology
echo Deploying space %2...
call "%XAP_HOME%\bin\gs" %INSIGHTEDGE_CMD% %2
exit /B
:deploy_space_topology
shift
rem Check if number of partitions (mandatory) is numric (see https://stackoverflow.com/a/17584764)
if [%2]==[] (echo Number of partitions must be specified when using --topology & goto deploy_space_usage)
SET "var="&for /f "delims=0123456789" %%i in ("%2") do set var=%%i
if defined var (echo Number of partitions is not an integer: %2 & goto deploy_space_usage)
set INSIGHTEDGE_CMD=%INSIGHTEDGE_CMD% -cluster schema=partitioned total_members=%2
shift
rem Check if number of backups (optional) is numric (see https://stackoverflow.com/a/17584764)
if [%2]==[] goto deploy_space_args
SET "var="&for /f "delims=0123456789" %%i in ("%2") do set var=%%i
if defined var goto deploy_space_args
set INSIGHTEDGE_CMD=%INSIGHTEDGE_CMD%,%2
shift
goto deploy_space_args
:deploy_space_usage
echo Usage: %~n0 deploy-space [--topology=p,b] space-name
exit /B

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
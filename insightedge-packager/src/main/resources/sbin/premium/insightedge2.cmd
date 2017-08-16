@echo off

call %~dp0..\..\bin\setenv.bat
rem Test that an argument was given
if "x%2"=="x" (
  echo Usage: insigthedge.cmd --mode ^(demo^|shutdown^)
  exit /b 1
)



set MODE=%2
set XAP_LOOKUP_LOCATORS=127.0.0.1:4174
set XAP_LOOKUP_GROUPS=insightedge
set NIC_ADDR=127.0.0.1
rem sets HADOOP_HOME if not specified by user - fixed NPE due to missing winutils
if "x%HADOOP_HOME%"=="x" (
  set HADOOP_HOME=%XAP_HOME%\insightedge\winutils
)

if "x%SPARK_HOME%"=="x" (
  set SPARK_HOME=%XAP_HOME%\insightedge\spark
)

for /f "tokens=1,2" %%a in (%XAP_HOME%\insightedge\VERSION) do (
  if "x%%a"=="xVersion:" (
    set VERSION=%%b
  )
  if "x%%a"=="xEdition:" (
    set EDITION=%%b
  )
)
 
echo    _____           _       _     _   ______    _            
echo   ^|_   _^|         ^(_^)     ^| ^|   ^| ^| ^|  ____^|  ^| ^|           
echo     ^| ^|  _ __  ___ _  __ _^| ^|__ ^| ^|_^| ^|__   __^| ^| __ _  ___ 
echo     ^| ^| ^| '_ \/ __^| ^|/ _` ^| '_ \^| __^|  __^| / _` ^|/ _` ^|/ _ \
echo    _^| ^|_^| ^| ^| \__ \ ^| ^(_^| ^| ^| ^| ^| ^|_^| ^|___^| ^(_^| ^| ^(_^| ^|  __/
echo   ^|_____^|_^| ^|_^|___/_^|\__, ^|_^| ^|_^|\__^|______\__,_^|\__, ^|\___^|
echo                       __/ ^|                       __/ ^|     
echo                      ^|___/                       ^|___/   version: %VERSION%
echo                                                          edition: %EDITION%

if "x%MODE%"=="xdemo" (
  echo --- Stopping Spark master
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd stop spark-master
  echo --- Spark master stopped
  echo --- Starting Spark master at 127.0.0.1
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd start spark-master spark\bin\spark-class org.apache.spark.deploy.master.Master --ip 127.0.0.1
  echo --- Spark master started

  rem prints a newline
  echo.
  echo --- Stopping Spark worker
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd stop spark-worker
  echo --- Spark worker stopped
  echo --- Starting Spark worker targetting spark://127.0.0.1:7077
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd start spark-worker spark\bin\spark-class org.apache.spark.deploy.worker.Worker spark://127.0.0.1:7077 --ip 127.0.0.1
  echo --- Spark worker started
    
  echo.
  echo --- Stopping Datagrid master
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd stop datagrid-master
  echo --- Datagrid master stopped
  echo --- Starting Gigaspaces datagrid management node ^(locator: 127.0.0.1:4174, group: insightedge^)
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd start datagrid-master ..\bin\gs-agent.bat gsa.gsc 0 gsa.global.gsm 0 gsa.gsm 1 gsa.global.lus 0 gsa.lus 1
  echo --- Gigaspaces datagrid management node started
  
  echo.
  echo --- Stopping Datagrid slave
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd stop datagrid-slave
  echo --- Datagrid slave stopped
  echo --- Starting Gigaspaces datagrid node ^(locator: 127.0.0.1:4174, group: insightedge, containers: 2^)
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd start datagrid-slave ..\bin\gs-agent.bat gsa.gsc 2 gsa.global.gsm 0 gsa.global.lus 0
  echo --- Gigaspaces datagrid node started
  
  echo.
  echo --- Deploying space: insightedge-space [2,0]  ^(locator: 127.0.0.1:4174, group: insightedge^)
  %XAP_HOME%\bin\gs.bat deploy-space -cluster schema=partitioned-sync2backup total_members=2,0 insightedge-space
  echo --- Done deploying space: insightedge-space
  
  echo.
  echo --- Stopping Zeppelin server
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd stop zeppelin
  echo --- Zeppelin server stopped
  echo --- Starting Zeppelin server
  rem add spark, datagrid and InsightEdge JARs to Zeppelin classpath
  set ZEPPELIN_CLASSPATH_OVERRIDES="%XAP_HOME%\insightedge\lib\*";"%XAP_HOME%\lib\required\*";"%XAP_HOME%\insightedge\spark\jars\*";
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd start zeppelin zeppelin\bin\zeppelin.cmd
  echo --- Zeppelin server can be accessed at http://127.0.0.1:9090
  
  echo.
  echo Demo steps:
  echo 1. make sure steps above were successfully executed
  echo 2. Open Web Notebook at http://127.0.0.1:9090 and run any of the available examples
  
  exit /b
)


if "x%MODE%"=="xshutdown" (
  echo --- Stopping Spark master
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd stop spark-master
  echo --- Spark master stopped
  
  echo.
  echo --- Stopping Spark worker
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd stop spark-worker
  echo --- Spark worker stopped
  
  echo.
  echo --- Stopping Datagrid master
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd stop datagrid-master
  echo --- Datagrid master stopped
  
  echo.
  echo --- Stopping Datagrid slave
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd stop datagrid-slave
  echo --- Datagrid slave stopped
  
  echo.
  echo --- Stopping Zeppelin server
  %XAP_HOME%\insightedge\sbin\win-daemon.cmd stop zeppelin
  echo --- Zeppelin server stopped
  exit /b
)


echo Invalid mode: %MODE%, expected demo^|shutdown
exit /b 1

@echo off

rem Test that an argument was given
if "x%2"=="x" (
  echo Usage: insigthedge.cmd --mode ^(demo^|shutdown^)
  exit /b 1
)

set MODE=%2
set INSIGHTEDGE_HOME=%~dp0..
set XAP_LOOKUP_LOCATORS=127.0.0.1:4174
set XAP_LOOKUP_GROUPS=insightedge
set NIC_ADDR=127.0.0.1
if "x%HADOOP_HOME%"=="x" (
  set HADOOP_HOME=%INSIGHTEDGE_HOME%\winutils
)

if "x%MODE%"=="xdemo" (
  echo --- Starting Spark master at 127.0.0.1
  %INSIGHTEDGE_HOME%\sbin\win-daemon.cmd start spark-master bin\spark-class org.apache.spark.deploy.master.Master --ip 127.0.0.1
  echo --- Spark master started
  
  echo.
  echo --- Starting Spark worker targetting spark://127.0.0.1:7077
  %INSIGHTEDGE_HOME%\sbin\win-daemon.cmd start spark-slave bin\spark-class org.apache.spark.deploy.worker.Worker spark://127.0.0.1:7077 --ip 127.0.0.1
  echo --- Spark worker started
    
  echo.
  echo --- Starting Gigaspaces datagrid management node ^(locator: 127.0.0.1:4174, group: insightedge^)
  %INSIGHTEDGE_HOME%\sbin\win-daemon.cmd start datagrid-master datagrid\bin\gs-agent.bat gsa.gsc 0 gsa.global.gsm 0 gsa.gsm 1 gsa.global.lus 0 gsa.lus 1
  echo --- Gigaspaces datagrid management node started
  
  echo.
  echo --- Starting Gigaspaces datagrid node ^(locator: 127.0.0.1:4174, group: insightedge, containers: 2^)
  %INSIGHTEDGE_HOME%\sbin\win-daemon.cmd start datagrid-slave datagrid\bin\gs-agent.bat gsa.gsc 2 gsa.global.gsm 0 gsa.global.lus 0
  echo --- Gigaspaces datagrid node started
  
  echo.
  echo --- Deploying space: insightedge-space [2,0]  ^(locator: 127.0.0.1:4174, group: insightedge^)
  %INSIGHTEDGE_HOME%\datagrid\bin\gs.bat deploy-space -cluster schema=partitioned-sync2backup total_members=2,0 insightedge-space
  echo --- Done deploying space: insightedge-space
  
  echo.
  echo --- Starting Zeppelin server
  rem %INSIGHTEDGE_HOME%\sbin\win-daemon.cmd start zeppelin <WIP>
  echo --- Zeppelin server can be accessed at http://127.0.0.1:8090
  
  echo.
  echo Demo steps:
  echo 1. make sure steps above were successfully executed
  echo 2. Open Web Notebook at http://127.0.0.1:8090 and run any of the available examples
  
  exit /b
)

if "x%MODE%"=="xshutdown" (
  echo --- Stopping Spark master
  %INSIGHTEDGE_HOME%\sbin\win-daemon.cmd stop spark-master
  echo --- Spark master stopped
  
  echo.
  echo --- Stopping Spark worker
  %INSIGHTEDGE_HOME%\sbin\win-daemon.cmd stop spark-slave
  echo --- Spark worker stopped
  
  echo.
  echo --- Stopping Datagrid master
  %INSIGHTEDGE_HOME%\sbin\win-daemon.cmd stop datagrid-master
  echo --- Datagrid master stopped
  
  echo.
  echo --- Stopping Datagrid slave
  %INSIGHTEDGE_HOME%\sbin\win-daemon.cmd stop datagrid-slave
  echo --- Datagrid slave stopped
  
  echo.
  echo --- Stopping Zeppelin server
  %INSIGHTEDGE_HOME%\sbin\win-daemon.cmd stop zeppelin
  echo --- Zeppelin server stopped
  exit /b
)

echo Invalid mode: %MODE%, expected demo^|shutdown
exit /b 1

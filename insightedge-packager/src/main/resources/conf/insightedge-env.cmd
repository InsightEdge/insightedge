@echo off
rem ***********************************************************************************************************
rem * This script is used to initialize common environment to GigaSpaces InsightEdge platform.                *
rem * It is highly recommended NOT TO MODIFY THIS SCRIPT, to simplify future upgrades.                        *
rem * If you need to override the defaults, please modify $XAP_HOME\bin\setenv-overrides.bat or set           *
rem * the XAP_SETTINGS_FILE environment variable to your custom script.                                       *
rem * For more information see http://docs.gigaspaces.com/xap/12.2/dev-java/common-environment-variables.html *
rem ***********************************************************************************************************
rem Source XAP environment:
call %~dp0..\..\bin\setenv.bat

rem Set InsightEdge defaults:
set INSIGHTEDGE_CLASSPATH="%XAP_HOME%\insightedge\lib\*;%XAP_HOME%\lib\required\*;%XAP_HOME%\lib\optional\spatial\*"
if defined INSIGHTEDGE_CLASSPATH_EXT set INSIGHTEDGE_CLASSPATH=%INSIGHTEDGE_CLASSPATH_EXT%;%INSIGHTEDGE_CLASSPATH%

if not defined HADOOP_HOME set HADOOP_HOME="%XAP_HOME%\insightedge\tools\winutils"
if not defined SPARK_HOME set SPARK_HOME="%XAP_HOME%\insightedge\spark"
if not defined SPARK_SUBMIT_OPTS set SPARK_SUBMIT_OPTS="-Dspark.driver.extraClassPath=%INSIGHTEDGE_CLASSPATH% -Dspark.executor.extraClassPath=%INSIGHTEDGE_CLASSPATH%"
if not defined SPARK_LOCAL_IP set SPARK_LOCAL_IP=%XAP_NIC_ADDRESS%

rem Zeppelin
if not defined ZEPPELIN_PORT set ZEPPELIN_PORT=9090
if not defined ZEPPELIN_INTP_CLASSPATH_OVERRIDES set ZEPPELIN_INTP_CLASSPATH_OVERRIDES=%INSIGHTEDGE_CLASSPATH%
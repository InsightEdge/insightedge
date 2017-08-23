@echo off
rem ***********************************************************************************************************
rem * This script is used to initialize common environment to GigaSpaces InsightEdge platform.                *
rem * It is highly recommended NOT TO MODIFY THIS SCRIPT, to simplify future upgrades.                        *
rem * If you need to override the defaults, please modify $XAP_HOME\bin\setenv-overrides.bat or set           *
rem * the XAP_SETTINGS_FILE environment variable to your custom script.                                       *
rem * For more information see http://docs.gigaspaces.com/xap/12.2/dev-java/common-environment-variables.html *
rem ***********************************************************************************************************
rem Source XAP environment:

@echo on

echo "START OF I9E - ENV --> all env:"
SET
echo "END all env"

echo "---- dp= [" %~dp0 "]"
echo "---- HADOOP_HOME= [" %HADOOP_HOME% "]"
echo "---- SPARK_HOME= [" %SPARK_HOME% "]"
echo "---- INSIGHTEDGE_CORE_CP= [" %INSIGHTEDGE_CORE_CP% "]"
echo "---- SPARK_SUBMIT_OPTS= [" %SPARK_SUBMIT_OPTS% "]"
echo "---- ZEPPELIN_INTP_CLASSPATH_OVERRIDES= [" %ZEPPELIN_INTP_CLASSPATH_OVERRIDES% "]"

@echo off

call %~dp0..\..\bin\setenv.bat
rem Set InsightEdge defaults:
if not defined HADOOP_HOME set HADOOP_HOME=%XAP_HOME%\insightedge\winutils

rem Set SPARK_HOME if not set
IF "%SPARK_HOME%"=="" set SPARK_HOME=%XAP_HOME%\insightedge\spark

set INSIGHTEDGE_CORE_CP=%XAP_HOME%\insightedge\lib\*;%XAP_HOME%\lib\required\*;%XAP_HOME%\lib\optional\spatial\*

rem Spark Submit
IF "%SPARK_SUBMIT_OPTS%"=="" set SPARK_SUBMIT_OPTS=-Dspark.driver.extraClassPath=%INSIGHTEDGE_CORE_CP% -Dspark.executor.extraClassPath=%INSIGHTEDGE_CORE_CP%

rem Zeppelin
set ZEPPELIN_INTP_CLASSPATH_OVERRIDES=%INSIGHTEDGE_CORE_CP%

@echo on

echo " #### END I9E - env "
echo "---- HADOOP_HOME= [" %HADOOP_HOME% "]"
echo "---- SPARK_HOME= [" %SPARK_HOME% "]"
echo "---- INSIGHTEDGE_CORE_CP= [" %INSIGHTEDGE_CORE_CP% "]"
echo "---- SPARK_SUBMIT_OPTS= [" %SPARK_SUBMIT_OPTS% "]"
echo "---- ZEPPELIN_INTP_CLASSPATH_OVERRIDES= [" %ZEPPELIN_INTP_CLASSPATH_OVERRIDES% "]"

@echo off

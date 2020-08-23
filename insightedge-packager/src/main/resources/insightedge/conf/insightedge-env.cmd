@echo off
rem ***********************************************************************************************************
rem * This script is used to initialize common environment to GigaSpaces InsightEdge platform.                *
rem * It is highly recommended NOT TO MODIFY THIS SCRIPT, to simplify future upgrades.                        *
rem * If you need to override the defaults, please modify $GS_HOME\bin\setenv-overrides.bat or set           *
rem * the GS_SETTINGS_FILE environment variable to your custom script.                                       *
rem * For more information see https://docs.gigaspaces.com/15.5/started/common-environment-variables.html     *
rem ***********************************************************************************************************
rem Source XAP environment;
call "%~dp0..\..\bin\setenv.bat"

rem Set InsightEdge defaults;
set INSIGHTEDGE_CLASSPATH=%GS_HOME%\insightedge\lib\*;%GS_HOME%\insightedge\lib\jdbc\*;%GS_HOME%\insightedge\lib\analytics-xtreme\*;%GS_HOME%\lib\required\*;%GS_HOME%\lib\optional\spatial\*

if defined INSIGHTEDGE_CLASSPATH_EXT set INSIGHTEDGE_CLASSPATH=%INSIGHTEDGE_CLASSPATH%;%INSIGHTEDGE_CLASSPATH_EXT%

if not defined HADOOP_HOME set HADOOP_HOME=%GS_HOME%\insightedge\tools\winutils
if not defined SPARK_HOME set SPARK_HOME=%GS_HOME%\insightedge\spark

rem InsightEdge dependencies to Spark
if not defined SPARK_DIST_CLASSPATH set SPARK_DIST_CLASSPATH=%INSIGHTEDGE_CLASSPATH%


rem Zeppelin
if not defined ZEPPELIN_PORT set ZEPPELIN_PORT=9090
rem Spark jars are added to interpreter classpath because of Analytics Xtreme
if not defined ZEPPELIN_INTP_CLASSPATH_OVERRIDES set ZEPPELIN_INTP_CLASSPATH_OVERRIDES=%INSIGHTEDGE_CLASSPATH%;"%SPARK_HOME%\jars\*"
if not defined ZEPPELIN_LOG_DIR set ZEPPELIN_LOG_DIR=%GS_HOME%\logs

if not defined INSIGHTEDGE_SPACE_NAME set INSIGHTEDGE_SPACE_NAME=demo

rem PYSPARK_PYTHON is also defined in insightedge-pyspark
if "x%PYSPARK_DRIVER_PYTHON%"=="x" (
  set PYSPARK_DRIVER_PYTHON=python
  if not [%PYSPARK_PYTHON%] == [] set PYSPARK_DRIVER_PYTHON=%PYSPARK_PYTHON%
)

rem PYTHONPATH is also defined in insightedge-pyspark
set PYTHONPATH=%SPARK_HOME%\python;%PYTHONPATH%
set PYTHONPATH=%SPARK_HOME%\python\lib\py4j-0.10.7-src.zip;%PYTHONPATH%

rem GS-14003 installing new interpreters fails due to Zeppelin accessing maven central via http
if not defined ZEPPELIN_INTERPRETER_DEP_MVNREPO set ZEPPELIN_INTERPRETER_DEP_MVNREPO=https://repo1.maven.org/maven2
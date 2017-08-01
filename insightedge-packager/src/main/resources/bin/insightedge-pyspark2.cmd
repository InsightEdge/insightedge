@echo off


call %~dp0..\..\bin\setenv.bat

rem Figure out where the Spark framework is installed

if "x%SPARK_HOME%"=="x" (
  set SPARK_HOME=%XAP_HOME%\insightedge\spark
)

call %SPARK_HOME%\bin\load-spark-env.cmd
set _SPARK_CMD_USAGE=Usage: bin\insightedge-pyspark.cmd [options]

rem Figure out which Python to use.
if "x%PYSPARK_DRIVER_PYTHON%"=="x" (
  set PYSPARK_DRIVER_PYTHON=python
  if not [%PYSPARK_PYTHON%] == [] set PYSPARK_DRIVER_PYTHON=%PYSPARK_PYTHON%
)

set PYTHONPATH=%SPARK_HOME%\python;%PYTHONPATH%
set PYTHONPATH=%SPARK_HOME%\python\lib\py4j-0.10.4-src.zip;%PYTHONPATH%

rem Load the InsighEdge version of shell.py script:
set OLD_PYTHONSTARTUP=%PYTHONSTARTUP%
set PYTHONSTARTUP=%SPARK_HOME%\bin\shell-init.py

call %XAP_HOME%\insightedge\bin\insightedge-submit2.cmd pyspark-shell-main --name "PySparkShell" %*

@echo off

rem Figure out where the Spark framework is installed
set SPARK_HOME=%~dp0..

call %SPARK_HOME%\bin\load-spark-env.cmd
set _SPARK_CMD_USAGE=Usage: bin\insightedge-pyspark.cmd [options]

rem Figure out which Python to use.
if "x%PYSPARK_DRIVER_PYTHON%"=="x" (
  set PYSPARK_DRIVER_PYTHON=python
  if not [%PYSPARK_PYTHON%] == [] set PYSPARK_DRIVER_PYTHON=%PYSPARK_PYTHON%
)

set PYTHONPATH=%SPARK_HOME%\python;%PYTHONPATH%
set PYTHONPATH=%SPARK_HOME%\python\lib\py4j-0.9-src.zip;%PYTHONPATH%

rem Load the InsighEdge version of shell.py script:
set OLD_PYTHONSTARTUP=%PYTHONSTARTUP%
set PYTHONSTARTUP=%SPARK_HOME%\bin\shell-init.py

call %SPARK_HOME%\bin\insightedge-submit2.cmd pyspark-shell-main --name "PySparkShell" %*

@echo off

call %~dp0..\conf\insightedge-env.cmd

rem Figure out where the Spark framework is installed

if "x%SPARK_HOME%"=="x" (
  set SPARK_HOME="%XAP_HOME%\insightedge\spark"
)

call %SPARK_HOME%\bin\load-spark-env.cmd
set _SPARK_CMD_USAGE=Usage: bin\insightedge-pyspark.cmd [options]

rem Figure out which Python to use.

rem PYSPARK_PYTHON is also defined in insightedge-env
if "x%PYSPARK_DRIVER_PYTHON%"=="x" (
  set PYSPARK_DRIVER_PYTHON=python
  if not [%PYSPARK_PYTHON%] == [] set PYSPARK_DRIVER_PYTHON=%PYSPARK_PYTHON%
)

rem PYTHONPATH is also defined in insightedge-env
set PYTHONPATH=%SPARK_HOME%\python;%PYTHONPATH%
set PYTHONPATH=%SPARK_HOME%\python\lib\py4j-*-src.zip;%PYTHONPATH%

rem Load the InsighEdge version of shell.py script:
set OLD_PYTHONSTARTUP=%PYTHONSTARTUP%
set PYTHONSTARTUP=%XAP_HOME%\insightedge\bin\shell-init.py

call "%SPARK_HOME%\bin\spark-submit" pyspark-shell-main --name "PySparkShell" %*

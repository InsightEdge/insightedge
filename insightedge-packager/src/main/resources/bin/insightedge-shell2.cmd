@echo off

set SPARK_HOME=%~dp0..
set INSIGHTEDGE_HOME=%~dp0..
set _SPARK_CMD_USAGE=Usage: .\bin\insightedge-shell.cmd [options]

rem SPARK-4161: scala does not assume use of the java classpath,
rem so we need to add the "-Dscala.usejavacp=true" flag manually. We
rem do this specifically for the Spark shell because the scala REPL
rem has its own class loader, and any additional classpath specified
rem through spark.driver.extraClassPath is not automatically propagated.
if "x%SPARK_SUBMIT_OPTS%"=="x" (
  set SPARK_SUBMIT_OPTS=-Dscala.usejavacp=true
  goto run_shell
)
set SPARK_SUBMIT_OPTS="%SPARK_SUBMIT_OPTS% -Dscala.usejavacp=true"

:run_shell
%INSIGHTEDGE_HOME%\bin\insightedge-submit2.cmd --class org.apache.spark.repl.Main --name "Spark shell" %* -i %INSIGHTEDGE_HOME%\bin\shell-init.scala
